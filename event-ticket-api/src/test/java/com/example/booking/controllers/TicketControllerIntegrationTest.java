package com.example.booking.controllers;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketRepository;
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
public class TicketControllerIntegrationTest {

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

        jwt = obtainJwt();

        var eventRequest = new CreateEventRequest(
                "Show do Legado",
                "22/04/2025",
                22,
                0,
                "Alfenas",
                30.0,
                List.of(
                        new CreateTicketCategoryRequest("VIP", 200.0, 2),
                        new CreateTicketCategoryRequest("Pista", 150.0, 3)
                )
        );

        String response = mockMvc.perform(post("/api/events")
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        eventId = UUID.fromString(objectMapper.readTree(response).get("eventId").asText());
    }

    private String obtainJwt() throws Exception {
        return Objects.requireNonNull(mockMvc.perform(post("/api/auth/signin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "admin",
                                          "password": "123"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getCookie("test-jwt"))
                .getValue();
    }

    @Test
    void testTicketLifecycle() throws Exception {
        var emmitRequest = new EmmitTicketRequest(eventId, "VIP");

        String ticket1Json = mockMvc.perform(post("/api/ticket")
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emmitRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID ticket1Id = UUID.fromString(objectMapper.readTree(ticket1Json).get("ticketId").asText());

        mockMvc.perform(get("/api/tickets")
                        .cookie(new Cookie("test-jwt", jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(1)));

        mockMvc.perform(post("/api/ticket")
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmmitTicketRequest(eventId, "Pista"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets")
                        .cookie(new Cookie("test-jwt", jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));

        mockMvc.perform(delete("/api/ticket/" + ticket1Id)
                        .cookie(new Cookie("test-jwt", jwt)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets")
                        .cookie(new Cookie("test-jwt", jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(1)));
    }

    @Test
    void testUserTicketsEndpoint() throws Exception {
        mockMvc.perform(post("/api/ticket")
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmmitTicketRequest(eventId, "VIP"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/ticket")
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmmitTicketRequest(eventId, "Pista"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/userTickets")
                        .cookie(new Cookie("test-jwt", jwt))
                        .param("page", "0").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(2)));
    }

    @Test
    void testAvailableTicketsByCategory() throws Exception {
        mockMvc.perform(get("/api/availableTickets/" + eventId)
                        .cookie(new Cookie("test-jwt", jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTickets").isArray())
                .andExpect(jsonPath("$.availableTickets[0].categoryName", is("VIP")))
                .andExpect(jsonPath("$.availableTickets[0].remainingTickets", is(2)))
                .andExpect(jsonPath("$.availableTickets[1].categoryName", is("Pista")))
                .andExpect(jsonPath("$.availableTickets[1].remainingTickets", is(3)));
    }
}
