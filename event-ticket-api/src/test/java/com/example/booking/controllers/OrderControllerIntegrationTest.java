package com.example.booking.controllers;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.request.CreateOrderRequest;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repository.*;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
public class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String API_BASE_URL = "/api";
    private static final String ORDER_URL = API_BASE_URL + "/order";
    private static final String ORDERS_URL = API_BASE_URL + "/orders";
    private static final String TICKET_URL = API_BASE_URL + "/ticket";
    private static final String EVENTS_URL = API_BASE_URL + "/events";
    private static final String AUTH_SIGNIN_URL = API_BASE_URL + "/auth/signin";
    private static final String JWT_COOKIE_NAME = "test-jwt";
    private static final String CATEGORY_VIP = "VIP";

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
    private TicketOrderRepository orderRepository;
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
    private UUID ticketId;

    @BeforeEach
    void setup() throws Exception {
        orderRepository.deleteAll();
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        ticketCategoryRepository.deleteAll();
        userRepository.deleteAll();

        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN não encontrada."));

        User adminUser = new User();

        adminUser.setUserName("admin");
        adminUser.setPassword(passwordEncoder.encode("123"));
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);

        this.jwt = obtainJwt();

        UUID eventId = createTestEvent();
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EntityNotFoundException("Event not found!"));

        Integer ticketCategoryId = event.getTicketCategories().getFirst().getTicketCategoryId();

        this.ticketId = createTestTicket(eventId, ticketCategoryId);
    }

    @Test
    void shouldCreateNewOrderSuccessfully() throws Exception {
        var createOrderRequest = new CreateOrderRequest(List.of(this.ticketId));

        mockMvc.perform(post(ORDER_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(ORDER_URL + "/")))
                .andExpect(jsonPath("$.orderId", notNullValue()))
                .andExpect(jsonPath("$.tickets.length()", is(1)))
                .andExpect(jsonPath("$.tickets[0].ticketId", is(notNullValue())))
                .andExpect(jsonPath("$.user.userName", is("admin")));
    }

    @Test
    void shouldReturnErrorWhenCreatingOrderWithNoTickets() throws Exception {
        var createOrderRequest = new CreateOrderRequest(List.of());

        mockMvc.perform(post(ORDER_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListUserOrders() throws Exception {
        createTestOrder(List.of(this.ticketId));

        mockMvc.perform(get(ORDERS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()", is(1)))
                .andExpect(jsonPath("$.orders[0].orderId", notNullValue()));
    }

    @Test
    void shouldDeleteOrderSuccessfully() throws Exception {
        UUID orderIdToDelete = createTestOrder(List.of(this.ticketId));

        mockMvc.perform(delete(ORDER_URL + "/" + orderIdToDelete)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ORDERS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()", is(0)));
    }

    private String obtainJwt() throws Exception {
        MvcResult result = mockMvc.perform(post(AUTH_SIGNIN_URL).contentType(MediaType.APPLICATION_JSON)
                .content(
                """
                {
                    "username": "admin",
                    "password": "123"
                }
                """)).andExpect(status().isOk()).andReturn();
        return Objects.requireNonNull(result.getResponse().getCookie(JWT_COOKIE_NAME)).getValue();
    }

    private UUID createTestEvent() throws Exception {
        var eventRequest = new CreateEventRequest("Evento para Pedidos", "25/12/2025", 20, 0, "Online", 300.0,
                List.of(new CreateTicketCategoryRequest(CATEGORY_VIP, 250.0, 10)));
        MvcResult result = mockMvc.perform(post(EVENTS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated()).andReturn();
        return getUuidFromMvcResult(result, "eventId");
    }

    private UUID createTestTicket(UUID eventId, Integer ticketCategoryId) throws Exception {
        var emmitRequest = new EmmitTicketRequest(eventId, ticketCategoryId);
        MvcResult result = mockMvc.perform(post(TICKET_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emmitRequest)))
                .andExpect(status().isCreated()).andReturn();
        return getUuidFromMvcResult(result, "ticketId");
    }

    private UUID createTestOrder(List<UUID> ticketIds) throws Exception {
        var createOrderRequest = new CreateOrderRequest(ticketIds);
        MvcResult result = mockMvc.perform(post(ORDER_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated()).andReturn();
        return getUuidFromMvcResult(result, "orderId");
    }

    private UUID getUuidFromMvcResult(MvcResult result, String fieldName) throws Exception {
        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);
        return UUID.fromString(root.get(fieldName).asText());
    }
}
