package com.example.booking.controllers;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.web.servlet.ResultActions;
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
@DisplayName("Testes de Integração para TicketController")
public class TicketControllerIntegrationTest {

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

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4-alpine")
            .withExposedPorts(6379);

    @MockitoBean
    private EventRequestProducerImpl eventPublisher;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private RedisTemplate<String, String> redisTemplate;

    private String jwt;
    private UUID eventId;

    @BeforeEach
    void setup() throws Exception {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        Assertions.assertNotNull(redisTemplate.getConnectionFactory());
        redisTemplate.getConnectionFactory().getConnection().serverCommands();

        this.jwt = obtainJwt();
        this.eventId = createTestEvent();
    }

    @Test
    void shouldEmmitNewTicketSuccessfully() throws Exception {
        emmitTicketRequest(CATEGORY_VIP)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketId", notNullValue()))
                .andExpect(jsonPath("$.ticketCategoryId", is(1)));
    }

    @Test
    void shouldListAllEmittedTickets() throws Exception {
        emmitTicketRequest(CATEGORY_VIP);
        emmitTicketRequest(CATEGORY_PISTA);

        mockMvc.perform(get(TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));
    }

    @Test
    void shouldListOnlyTicketsForAuthenticatedUser() throws Exception {
        emmitTicketRequest(CATEGORY_VIP);
        emmitTicketRequest(CATEGORY_PISTA);

        mockMvc.perform(get(USER_TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));
    }

    @Test
    void shouldDeleteTicketAndRemoveItFromList() throws Exception {
        MvcResult result = emmitTicketRequest(CATEGORY_VIP).andReturn();
        UUID ticketIdToDelete = getUuidFromMvcResult(result, "ticketId");

        emmitTicketRequest(CATEGORY_PISTA);

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
        emmitTicketRequest(CATEGORY_VIP);

        mockMvc.perform(get(AVAILABLE_TICKETS_URL + "/" + eventId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTickets").isArray())
                .andExpect(jsonPath("$.availableTickets[?(@.categoryName == 'VIP')].remainingTickets", contains(1)))
                .andExpect(jsonPath("$.availableTickets[?(@.categoryName == 'Pista')].remainingTickets", contains(3)));
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

    private ResultActions emmitTicketRequest(String category) throws Exception {
        var emmitRequest = new EmmitTicketRequest(eventId, category);

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