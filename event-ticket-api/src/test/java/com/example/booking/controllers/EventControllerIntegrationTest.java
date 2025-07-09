package com.example.booking.controllers;

import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.messaging.EventRequestProducerImpl;
import com.example.booking.repositories.RoleRepository;
import com.example.booking.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EventControllerIntegrationTest extends AbstractIntegrationTest {
    @MockitoBean
    private EventRequestProducerImpl eventPublisher;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role Admin not found!"));

        User adminUser = new User();
        adminUser.setUserName("admin");
        adminUser.setPassword(passwordEncoder.encode("123"));
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of(adminRole));
        userRepository.save(adminUser);

        this.jwt = obtainJwt();
    }

    @Test
    void shouldCreateEventSuccessfully() throws Exception {
        CreateEventRequest request = createSampleEventRequest();

        mockMvc.perform(post("/api/events")
                        .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventName").value("Show do Legado"));
    }

    @Test
    void shouldReturnEmptyTrendingEventsInitially() throws Exception {
        mockMvc.perform(get("/api/events/trending").cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldListNoUserEventsWhenNoneAreCreated() throws Exception {
        mockMvc.perform(get("/api/events/my-events").cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void shouldListUserEventsWhenCreated() throws Exception {
        CreateEventRequest request = createSampleEventRequest();
        mockMvc.perform(post("/api/events")
                        .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events/my-events").cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isNotEmpty());
    }

    @Test
    void shouldDeleteEventSuccessfully() throws Exception {
        CreateEventRequest request = createSampleEventRequest();
        String content = mockMvc.perform(post("/api/events")
                        .cookie(new Cookie(BOOKING_JWT_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();
        UUID eventId = getUuidFromResponse(content);

        mockMvc.perform(delete("/api/events/" + eventId).cookie(new Cookie(BOOKING_JWT_NAME, jwt)))
                .andExpect(status().isNoContent());
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

    private CreateEventRequest createSampleEventRequest() {
        String futureDate = LocalDate.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return new CreateEventRequest(
                "Show do Legado", futureDate, 22, 0, "Alfenas", 30.0,
                List.of(new CreateTicketCategoryRequest("VIP", 200.0, 100))
        );
    }

    private UUID getUuidFromResponse(String jsonResponse) throws Exception {
        return UUID.fromString(objectMapper.readTree(jsonResponse).get("eventId").asText());
    }
}