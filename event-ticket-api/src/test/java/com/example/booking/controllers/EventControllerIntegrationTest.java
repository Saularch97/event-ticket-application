package com.example.booking.controllers;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("eureka.client.enabled", () -> "false");

    }

    @MockitoBean
    private EventRequestProducerImpl eventPublisher;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void cleanUp() {
        eventRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll(); // limpa Redis
    }

    @Test
    void testCreateEvent() throws Exception {

        String jwt = mockMvc.perform(post("/api/auth/signin")
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
                .getCookie("booking-jwt") // ou getHeader("Authorization")
                .getValue();


        CreateEventRequest request = new CreateEventRequest(
    "Show do Legado",
    "22/04/2025",
    22,
    0,
    "Alfenas",
    30.0,
            List.of(
                new CreateTicketCategoryRequest("VIP", 200.0, 100),
                new CreateTicketCategoryRequest("Pista", 300.0, 100)
            )
        );

        mockMvc.perform(post("/api/events")
            .cookie(new Cookie("booking-jwt", jwt))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eventName").value("Show do Legado"))
            .andExpect(jsonPath("$.availableTickets").value(200));
    }

    @Test
    void testListTrendingEventsInitiallyEmpty() throws Exception {

        String jwt = mockMvc.perform(post("/api/auth/signin")
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
                .getCookie("booking-jwt") // ou getHeader("Authorization")
                .getValue();


        mockMvc.perform(get("/api/trending").cookie(new Cookie("booking-jwt", jwt)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void testDeleteEvent() throws Exception {

        String jwt = mockMvc.perform(post("/api/auth/signin")
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
                .getCookie("booking-jwt") // ou getHeader("Authorization")
                .getValue();

        // Primeiro cria o evento
        CreateEventRequest request = new CreateEventRequest(
                "Show do Legado",
                "22/04/2025",
                22,
                0,
                "Alfenas",
                30.0,
                List.of(
                        new CreateTicketCategoryRequest("VIP", 200.0, 100),
                        new CreateTicketCategoryRequest("Pista", 300.0, 100)
                )
        );


        String content = mockMvc.perform(post("/api/events")
                        .cookie(new Cookie("booking-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        // Extrai o ID do evento da resposta
        String eventId = objectMapper.readTree(content).get("eventId").asText();

        // Deleta o evento
        mockMvc.perform(delete("/api/events/" + eventId).cookie(new Cookie("booking-jwt", jwt)))
                .andExpect(status().isNoContent());
    }
}
