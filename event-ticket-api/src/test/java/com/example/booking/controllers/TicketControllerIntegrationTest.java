package com.example.booking.controllers;

import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.controller.request.ticket.EmmitTicketRequest;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repositories.RoleRepository;
import com.example.booking.repositories.TicketCategoryRepository;
import com.example.booking.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TicketControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String API_BASE_URL = "/api";
    private static final String TICKET_URL = API_BASE_URL + "/tickets";
    private static final String TICKETS_URL = API_BASE_URL + "/tickets";
    private static final String USER_TICKETS_URL = API_BASE_URL + "/tickets/my-tickets";
    private static final String EVENTS_URL = API_BASE_URL + "/events";
    private static final String AUTH_SIGNIN_URL = API_BASE_URL + "/auth/signin";
    private static final String JWT_COOKIE_NAME = "booking-test-jwt";
    private static final String CATEGORY_VIP = "VIP";
    private static final String CATEGORY_PISTA = "Pista";

    @MockitoBean
    private EventRequestProducerImpl eventPublisher;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TicketCategoryRepository ticketCategoryRepository;

    private String jwt;
    private UUID eventId;
    private Integer vipCategoryId;
    private Integer pistaCategoryId;

    @BeforeEach
    void setup() throws Exception {

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
        List<TicketCategory> categories = ticketCategoryRepository.findAllTicketCategoriesByEventId(this.eventId);
        this.vipCategoryId = findCategoryIdByName(categories, CATEGORY_VIP);
        this.pistaCategoryId = findCategoryIdByName(categories, CATEGORY_PISTA);
    }

    @Test
    void shouldEmmitNewTicketSuccessfully() throws Exception {
        emmitTicketRequest(this.vipCategoryId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId", notNullValue()))
                .andExpect(jsonPath("$.ticketCategoryId", is(this.vipCategoryId)));
    }

    @Test
    void shouldListAllEmittedTickets() throws Exception {
        emmitTicketRequest(this.vipCategoryId);
        emmitTicketRequest(this.pistaCategoryId);

        mockMvc.perform(get(TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));
    }

    @Test
    void shouldListOnlyTicketsForAuthenticatedUser() throws Exception {
        emmitTicketRequest(this.vipCategoryId);
        emmitTicketRequest(this.pistaCategoryId);

        mockMvc.perform(get(USER_TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));
    }

    private String obtainJwt() throws Exception {
        MvcResult result = mockMvc.perform(post(AUTH_SIGNIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\",\"password\": \"123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return Objects.requireNonNull(result.getResponse().getCookie(JWT_COOKIE_NAME)).getValue();
    }

    private UUID createTestEvent() throws Exception {
        String futureDate = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        var eventRequest = new CreateEventRequest(
                "Show do Legado", futureDate, 22, 0, "Alfenas", 30.0,
                List.of(
                        new CreateTicketCategoryRequest(CATEGORY_VIP, 200.0, 2),
                        new CreateTicketCategoryRequest(CATEGORY_PISTA, 150.0, 3)
                )
        );
        MvcResult result = mockMvc.perform(post(EVENTS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated()).andReturn();
        return getUuidFromMvcResult(result);
    }

    private ResultActions emmitTicketRequest(Integer ticketCategoryId) throws Exception {
        var emmitRequest = new EmmitTicketRequest(eventId, ticketCategoryId);
        return mockMvc.perform(post(TICKET_URL)
                .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emmitRequest)));
    }

    private Integer findCategoryIdByName(List<TicketCategory> categories, String name) {
        return categories.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .map(TicketCategory::getTicketCategoryId)
                .orElseThrow(() -> new IllegalStateException("Categoria " + name + " não encontrada."));
    }

    private UUID getUuidFromMvcResult(MvcResult result) throws Exception {
        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);
        return UUID.fromString(root.get("eventId").asText());
    }
}
