package com.example.booking.controllers;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.request.CreateOrderRequest;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketOrderRepository;
import com.example.booking.repository.TicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
public class OrderControllerIntegrationTest {

    private static final String API_BASE_URL = "/api";
    private static final String ORDER_URL = API_BASE_URL + "/order";
    private static final String ORDERS_URL = API_BASE_URL + "/orders";
    private static final String TICKET_URL = API_BASE_URL + "/ticket";
    private static final String EVENTS_URL = API_BASE_URL + "/events";
    private static final String AUTH_SIGNIN_URL = API_BASE_URL + "/auth/signin";
    private static final String JWT_COOKIE_NAME = "test-jwt";
    private static final String CATEGORY_VIP = "VIP";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16").withDatabaseName("testdb").withUsername("test").withPassword("test");
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4-alpine").withExposedPorts(6379);
    @MockitoBean
    private EventRequestProducerImpl eventPublisher;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private TicketOrderRepository orderRepository; // Repositório de Pedidos
    @Autowired private RedisTemplate<String, String> redisTemplate;

    private String jwt;
    private UUID ticketId;

    @BeforeEach
    void setup() throws Exception {
        orderRepository.deleteAll();
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        this.jwt = obtainJwt();
        UUID eventId = createTestEvent();
        this.ticketId = createTestTicket(eventId);
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
                .andExpect(jsonPath("$.tickets[0].ticketId", is(this.ticketId.toString())))
                .andExpect(jsonPath("$.user.userName", is("admin")));
    }

    @Test
    void shouldReturnErrorWhenCreatingOrderWithNoTickets() throws Exception {
        var createOrderRequest = new CreateOrderRequest(List.of()); // Lista de tickets vazia

        mockMvc.perform(post(ORDER_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest()); // Validação do @Valid e @Size(min=1)
    }

    @Test
    void shouldListUserOrders() throws Exception {
        // Cria um pedido para ter o que listar
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
        MvcResult result = mockMvc.perform(post(AUTH_SIGNIN_URL).contentType(MediaType.APPLICATION_JSON).content("{\"username\": \"admin\",\"password\": \"123\"}")).andExpect(status().isOk()).andReturn();
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

    private UUID createTestTicket(UUID eventId) throws Exception {
        var emmitRequest = new EmmitTicketRequest(eventId, OrderControllerIntegrationTest.CATEGORY_VIP);
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
