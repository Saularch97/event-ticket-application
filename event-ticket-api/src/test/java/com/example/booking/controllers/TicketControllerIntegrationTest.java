package com.example.booking.controllers;

import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.controller.request.ticket.EmmitTicketRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
public class TicketControllerIntegrationTest extends AbstractIntegrationTest{

    private static final String API_BASE_URL = "/api";
    private static final String TICKET_URL = API_BASE_URL + "/ticket";
    private static final String TICKETS_URL = API_BASE_URL + "/tickets";
    private static final String USER_TICKETS_URL = API_BASE_URL + "/userTickets";
    private static final String AVAILABLE_TICKETS_URL = API_BASE_URL + "/availableTickets";
    private static final String EVENTS_URL = API_BASE_URL + "/events";
    private static final String AUTH_SIGNIN_URL = API_BASE_URL + "/auth/signin";
    private static final String JWT_COOKIE_NAME = "test-jwt";
    private static final String CATEGORY_VIP = "VIP";
    private static final String CATEGORY_PISTA = "Pista";
    private static final String GET_TICKETS_BY_CATEGORY_URL = API_BASE_URL + "/getTicketsByCategoryId";

    @MockitoBean
    private EventRequestProducerImpl eventPublisher;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired TicketCategoryRepository ticketCategoryRepository;

    private String jwt;
    private UUID eventId;
    private Integer vipCategoryId;
    private Integer pistaCategoryId;

    @BeforeEach
    void setup() throws Exception {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        ticketCategoryRepository.deleteAll();
        userRepository.deleteAll();

        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN não encontrada."));

        User adminUser = new User();

        adminUser.setUserName("admin");
        adminUser.setPassword(passwordEncoder.encode("123"));
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);

        this.jwt = obtainJwt();

        this.eventId = createTestEvent();

        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EntityNotFoundException("Event not found!"));
        vipCategoryId = event.getTicketCategories().stream().filter(ticketCategory -> ticketCategory.getName().equals(CATEGORY_VIP)).findFirst().get().getTicketCategoryId();
        pistaCategoryId = event.getTicketCategories().stream().filter(ticketCategory -> ticketCategory.getName().equals(CATEGORY_PISTA)).findFirst().get().getTicketCategoryId();
    }

    @Test
    void shouldEmmitNewTicketSuccessfully() throws Exception {
        emmitTicketRequest(vipCategoryId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId", notNullValue()))
                .andExpect(jsonPath("$.ticketCategoryId", is(vipCategoryId)));
    }

    @Test
    void shouldListAllEmittedTickets() throws Exception {
        emmitTicketRequest(vipCategoryId);
        emmitTicketRequest(pistaCategoryId);

        mockMvc.perform(get(TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));
    }

    @Test
    void shouldListOnlyTicketsForAuthenticatedUser() throws Exception {
        emmitTicketRequest(vipCategoryId);
        emmitTicketRequest(pistaCategoryId);

        mockMvc.perform(get(USER_TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));
    }

    @Test
    void shouldDeleteTicketAndRemoveItFromList() throws Exception {
        MvcResult result = emmitTicketRequest(vipCategoryId).andReturn();
        UUID ticketIdToDelete = getUuidFromMvcResult(result, "ticketId");

        emmitTicketRequest(vipCategoryId);

        mockMvc.perform(delete(TICKET_URL + "/" + ticketIdToDelete)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(1)));
    }

    @Test
    void shouldReturnCorrectCountOfAvailableTicketsByCategory() throws Exception {
        emmitTicketRequest(vipCategoryId);

        mockMvc.perform(get(AVAILABLE_TICKETS_URL + "/" + eventId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTickets").isArray())
                .andExpect(jsonPath("$.availableTickets[?(@.categoryName == 'VIP')].remainingTickets", contains(1)))
                .andExpect(jsonPath("$.availableTickets[?(@.categoryName == 'Pista')].remainingTickets", contains(3)));
    }

    @Test
    void shouldReturnCorrectTheTicketsByCategoryId() throws Exception {
        emmitTicketRequest(vipCategoryId);

        mockMvc.perform(get(GET_TICKETS_BY_CATEGORY_URL + "/" + vipCategoryId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets").isNotEmpty())
                .andExpect(jsonPath("$.tickets[0].ticketId").isNotEmpty())
                .andExpect(jsonPath("$.tickets[0].eventId").isNotEmpty())
                .andExpect(jsonPath("$.tickets[0].userId").isNotEmpty())
                .andExpect(jsonPath("$.tickets[0].ticketCategoryId").value(vipCategoryId));

    }

    private String obtainJwt() throws Exception {
        MvcResult result = mockMvc.perform(post(AUTH_SIGNIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return Objects.requireNonNull(result.getResponse().getCookie(JWT_COOKIE_NAME)).getValue();
    }

    private UUID createTestEvent() throws Exception {
        var eventRequest = new CreateEventRequest(
                "Show do Legado",
                "22/04/2025",
                22,
                0,
                "Alfenas",
                30.0,
                List.of(
                        new CreateTicketCategoryRequest(CATEGORY_VIP, 200.0, 2),
                        new CreateTicketCategoryRequest(CATEGORY_PISTA, 150.0, 3)
                )
        );

        MvcResult result = mockMvc.perform(post(EVENTS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return getUuidFromMvcResult(result, "eventId");
    }

    private ResultActions emmitTicketRequest(Integer ticketCategoryId) throws Exception {
        var emmitRequest = new EmmitTicketRequest(eventId, ticketCategoryId);

        return mockMvc.perform(post(TICKET_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emmitRequest)));
    }

    private UUID getUuidFromMvcResult(MvcResult result, String fieldName) throws Exception {
        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);
        return UUID.fromString(root.get(fieldName).asText());
    }
}
