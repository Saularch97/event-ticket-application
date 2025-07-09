package com.example.booking.controllers;

import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.event.UpdateEventRequest;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
class EventControllerIntegrationTest extends AbstractIntegrationTest{

    @MockitoBean
    private EventRequestProducerImpl eventPublisher;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String jwt;
    private static final String BOOKING_JWT_NAME = "booking-test-jwt";

    @BeforeEach
    public void setup() throws Exception {
        refreshTokenRepository.deleteAll();
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();


        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN não encontrada."));
        User adminUser = new User();
        adminUser.setUserName("admin");
        adminUser.setPassword(passwordEncoder.encode("123"));
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);

        this.jwt = obtainJwt();
    }

    @Test
    void testCreateEvent() throws Exception {
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
            .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eventName").value("Show do Legado"))
            .andExpect(jsonPath("$.availableTickets").value(200));
    }

    @Test
    void testListTrendingEventsInitiallyEmpty() throws Exception {
        mockMvc.perform(get("/api/events/trending").cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listAllAvailableUserEventsWhenNoEventsAreCreated() throws Exception {
        mockMvc.perform(get("/api/events/my-events").cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void listAllAvailableUserEventsWhenEventsAreCreated() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Show do Legado",
                "22/04/2025",
                22,
                0,
                "Alfenas",
                30.0,
                List.of(
                        new CreateTicketCategoryRequest("VIP", 200.0, 500),
                        new CreateTicketCategoryRequest("Pista", 300.0, 500)
                )
        );

        mockMvc.perform(post("/api/events")
                        .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventName").value("Show do Legado"))
                .andExpect(jsonPath("$.availableTickets").value(1000));

        mockMvc.perform(get("/api/events/my-events").cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isNotEmpty());
    }

    @Test
    void testDeleteEvent() throws Exception {
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
                        .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String eventId = objectMapper.readTree(content).get("eventId").asText();

        mockMvc.perform(delete("/api/events/" + eventId).cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isNoContent());
    }


    @Test
    void testUpdateEvent() throws Exception {
        CreateEventRequest createRequest = new CreateEventRequest(
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

        String createdEventJson = mockMvc.perform(post("/api/events")
                        .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID eventId = UUID.fromString(objectMapper.readTree(createdEventJson).get("eventId").asText());

        LocalDateTime newDate = LocalDateTime.now().plusYears(1).withHour(23).withMinute(30);

        UpdateEventRequest updateRequest = new UpdateEventRequest(
                "Show do Legado - Edição Especial",
                newDate,
                "São Paulo",
                300.0,
                List.of(
                        new CreateTicketCategoryRequest("VIP", 200.0, 100),
                        new CreateTicketCategoryRequest("Pista", 300.0, 100)
                )
        );

        mockMvc.perform(put("/api/events/" + eventId)
                        .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/events").cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].name").value("Show do Legado - Edição Especial"))
                .andExpect(jsonPath("$.events[0].location").value("São Paulo"));
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
                        .getCookie(BOOKING_JWT_NAME))
                .getValue();
    }
}
