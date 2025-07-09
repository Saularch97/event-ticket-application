package com.example.booking.controllers;

import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.event.UpdateEventRequest;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repositories.EventRepository;
import com.example.booking.repositories.RoleRepository;
import com.example.booking.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
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
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup()  {
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role ADMIN não encontrada."));
        User adminUser = new User();
        adminUser.setUserName("admin");
        adminUser.setPassword(passwordEncoder.encode("123"));
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);
    }

    @Test
    void testCreateEvent() throws Exception {

        String jwt = Objects.requireNonNull(mockMvc.perform(post("/api/auth/signin")
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
            .cookie(new Cookie("test-jwt", jwt))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eventName").value("Show do Legado"))
            .andExpect(jsonPath("$.availableTickets").value(200));
    }

    @Test
    void testListTrendingEventsInitiallyEmpty() throws Exception {

        String jwt = Objects.requireNonNull(mockMvc.perform(post("/api/auth/signin")
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
                        .getCookie("test-jwt")) // ou getHeader("Authorization")
                .getValue();


        mockMvc.perform(get("/api/trending").cookie(new Cookie("test-jwt", jwt)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listAllAvailableUserEventsWhenNoEventsAreCreated() throws Exception {

        String jwt = Objects.requireNonNull(mockMvc.perform(post("/api/auth/signin")
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
                        .getCookie("test-jwt")) // ou getHeader("Authorization")
                .getValue();


        mockMvc.perform(get("/api/availableUserEvents").cookie(new Cookie("test-jwt", jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void listAllAvailableUserEventsWhenEventsAreCreated() throws Exception {

        String jwt = Objects.requireNonNull(mockMvc.perform(post("/api/auth/signin")
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
                        .getCookie("test-jwt")) // ou getHeader("Authorization")
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
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventName").value("Show do Legado"))
                .andExpect(jsonPath("$.availableTickets").value(200));

        mockMvc.perform(get("/api/availableUserEvents").cookie(new Cookie("test-jwt", jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isNotEmpty());
    }

    @Test
    void testDeleteEvent() throws Exception {

        String jwt = Objects.requireNonNull(mockMvc.perform(post("/api/auth/signin")
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
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String eventId = objectMapper.readTree(content).get("eventId").asText();

        mockMvc.perform(delete("/api/events/" + eventId).cookie(new Cookie("test-jwt", jwt)))
                .andExpect(status().isNoContent());
    }


    @Test
    void testUpdateEvent() throws Exception {

        String jwt = Objects.requireNonNull(mockMvc.perform(post("/api/auth/signin")
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
                        .cookie(new Cookie("test-jwt", jwt))
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
                        .cookie(new Cookie("test-jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/events").cookie(new Cookie("test-jwt", jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].name").value("Show do Legado - Edição Especial"))
                .andExpect(jsonPath("$.events[0].location").value("São Paulo"));
    }
}
