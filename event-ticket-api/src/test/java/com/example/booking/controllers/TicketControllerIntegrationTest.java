package com.example.booking.controllers;

import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.ticket.CheckInRequest; // Import Adicionado
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.controller.request.ticket.EmmitTicketRequest;
import com.example.booking.domain.entities.*; // Import genérico para Ticket
import com.example.booking.domain.enums.ERole;
import com.example.booking.domain.enums.ETicketStatus;
import com.example.booking.messaging.producer.EventRequestProducerImpl;
import com.example.booking.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
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
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketRepository ticketRepository;

    private String jwt;
    private UUID eventId;
    private Long vipCategoryId;
    private Long pistaCategoryId;

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
                .andExpect(jsonPath("$.ticketCategoryId", is(this.vipCategoryId.intValue())));
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

    @Test
    void shouldDecrementAvailableTickets_WhenTicketIsEmitted() throws Exception {
        TicketCategory categoryBefore = ticketCategoryRepository.findById(this.vipCategoryId).orElseThrow();
        Event eventBefore = eventRepository.findById(this.eventId).orElseThrow();

        int initialCategoryCount = categoryBefore.getAvailableCategoryTickets();
        int initialEventCount = eventBefore.getAvailableTickets();

        emmitTicketRequest(this.vipCategoryId).andExpect(status().isCreated());

        TicketCategory categoryAfter = ticketCategoryRepository.findById(this.vipCategoryId).orElseThrow();
        Event eventAfter = eventRepository.findById(this.eventId).orElseThrow();

        assertThat(categoryAfter.getAvailableCategoryTickets(), is(initialCategoryCount - 1));
        assertThat(eventAfter.getAvailableTickets(), is(initialEventCount - 1));
    }


    @Test
    void shouldDeleteTicketSuccessfully() throws Exception {
        MvcResult result = emmitTicketRequest(this.vipCategoryId)
                .andExpect(status().isCreated())
                .andReturn();
        UUID ticketId = getUuidFromMvcResult(result, "ticketId");

        mockMvc.perform(delete(TICKET_URL + "/" + ticketId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(USER_TICKETS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(0)));
    }

    @Test
    void shouldGetAvailableTicketsForEvent() throws Exception {
        mockMvc.perform(get(TICKET_URL + "/available/" + this.eventId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableTickets", hasSize(2))) // VIP e Pista
                .andExpect(jsonPath("$.availableTickets[?(@.categoryName == 'VIP')].remainingTickets", contains(2)))
                .andExpect(jsonPath("$.availableTickets[?(@.categoryName == 'Pista')].remainingTickets", contains(3)));
    }

    @Test
    void shouldGetTicketsByCategoryId() throws Exception {
        emmitTicketRequest(this.vipCategoryId);
        emmitTicketRequest(this.pistaCategoryId);

        mockMvc.perform(get(TICKET_URL + "/category/" + this.vipCategoryId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets.length()", is(1)))
                .andExpect(jsonPath("$.tickets[0].ticketCategoryId", is(this.vipCategoryId.intValue())));
    }

    @Test
    void shouldGenerateQrCodeImage() throws Exception {
        MvcResult result = emmitTicketRequest(this.vipCategoryId).andReturn();
        UUID ticketId = getUuidFromMvcResult(result, "ticketId");

        mockMvc.perform(get(TICKET_URL + "/" + ticketId + "/qrcode")
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void shouldVerifyTicketValidity() throws Exception {
        MvcResult result = emmitTicketRequest(this.vipCategoryId).andReturn();
        UUID ticketId = getUuidFromMvcResult(result, "ticketId");

        mockMvc.perform(get(TICKET_URL + "/verify/" + ticketId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId", is(ticketId.toString())))
                .andExpect(jsonPath("$.valid", is(false)));
    }

    @Test
    void shouldCheckInTicketSuccessfully() throws Exception {
        MvcResult result = emmitTicketRequest(this.vipCategoryId).andReturn();
        UUID ticketId = getUuidFromMvcResult(result, "ticketId");

        mockMvc.perform(get(TICKET_URL + "/" + ticketId + "/qrcode")
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt)))
                .andExpect(status().isOk());

        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();

        ticket.setTicketStatus(ETicketStatus.PAID);
        ticketRepository.save(ticket);

        String validationCode = ticket.getValidationCode();
        CheckInRequest checkInRequest = new CheckInRequest(validationCode);

        mockMvc.perform(post(TICKET_URL + "/checkin/" + ticketId)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isOk());
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
                "Show do Legado", futureDate, 22, 0, "Alfenas", BigDecimal.valueOf(30.0),
                List.of(
                        new CreateTicketCategoryRequest(CATEGORY_VIP, BigDecimal.valueOf(200.0), 2),
                        new CreateTicketCategoryRequest(CATEGORY_PISTA, BigDecimal.valueOf(150.0), 3)
                )
        );
        MvcResult result = mockMvc.perform(post(EVENTS_URL)
                        .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventRequest)))
                .andExpect(status().isCreated()).andReturn();
        return getUuidFromMvcResult(result, "eventId");
    }

    private ResultActions emmitTicketRequest(Long ticketCategoryId) throws Exception {
        var emmitRequest = new EmmitTicketRequest(eventId, ticketCategoryId);
        return mockMvc.perform(post(TICKET_URL)
                .cookie(new Cookie(JWT_COOKIE_NAME, jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emmitRequest)));
    }

    private Long findCategoryIdByName(List<TicketCategory> categories, String name) {
        return categories.stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .map(TicketCategory::getTicketCategoryId)
                .orElseThrow(() -> new IllegalStateException("Categoria " + name + " não encontrada."));
    }

    private UUID getUuidFromMvcResult(MvcResult result, String fieldName) throws Exception {
        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(jsonResponse);
        return UUID.fromString(root.get(fieldName).asText());
    }
}