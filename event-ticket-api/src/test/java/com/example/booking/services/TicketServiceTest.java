package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.dto.RemainingTicketCategoryDto;
import com.example.booking.dto.TicketItemDto;
import com.example.booking.dto.TicketsDto;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.domain.entities.*;
import com.example.booking.repositories.TicketRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final String CATEGORY_NAME = "Pista Premium";
    private static final String TEST_LOCATION = "Estádio Nacional";

    @Mock private TicketRepository ticketRepository;
    @Mock private UserService userService;
    @Mock private EventsService eventService;
    @Mock private JwtUtils jwtUtils;
    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks private TicketServiceImpl ticketsService;

    private User testUser;
    private Event testEvent;
    private TicketCategory testCategory;
    private UUID testEventId;
    private Integer testTicketCategoryId;
    private UUID testUserId;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = "testUser";
        testUserId = UUID.randomUUID();
        testEventId = UUID.randomUUID();
        testTicketCategoryId = 1;

        testUser = new User();
        testUser.setUserId(testUserId);
        testUser.setUserName(testUsername);

        testCategory = new TicketCategory();
        testCategory.setTicketCategoryId(testTicketCategoryId);
        testCategory.setName(CATEGORY_NAME);
        testCategory.setPrice(250.0);
        testCategory.setAvailableCategoryTickets(300);

        testEvent = new Event();
        testEvent.setEventId(testEventId);
        testEvent.setEventLocation(TEST_LOCATION);
        testEvent.setEventDate(LocalDateTime.of(2025, 12, 15, 20, 0));
        testEvent.setTicketCategories(List.of(testCategory));
        testEvent.setAvailableTickets(500);
    }

    @Test
    void emmitTicket_ShouldReturnTicketItemDto_WhenRequestIsValid() {
        EmmitTicketRequest request = new EmmitTicketRequest(testEventId, testTicketCategoryId);

        when(jwtUtils.getAuthenticatedUsername()).thenReturn(testUsername);
        when(userService.findUserEntityByUserName(testUsername)).thenReturn(testUser);
        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);
        when(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).thenReturn(cache);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketItemDto result = ticketsService.emmitTicket(request);

        assertNotNull(result);
        assertEquals(testEvent.getOriginalAmountOfTickets() - 1, testEvent.getAvailableTickets());
    }

    @Test
    void emmitTicket_ShouldThrowIllegalArgumentException_WhenCategoryNotFound() {
        EmmitTicketRequest request = new EmmitTicketRequest(testEventId, 2);

        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ticketsService.emmitTicket(request));

        assertEquals("Ticket category not found for id: 2", exception.getMessage());

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void emmitTicket_ShouldThrowException_WhenNoTicketsAvailableInCategory() {
        testEvent.getTicketCategories().getFirst().setAvailableCategoryTickets(0);
        EmmitTicketRequest request = new EmmitTicketRequest(testEventId, testTicketCategoryId);

        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);

        assertThrows(IllegalStateException.class, () -> ticketsService.emmitTicket(request));

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void deleteEmittedTicket_ShouldDeleteTicketAndUpdateCounts_WhenTicketExists() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setEvent(testEvent);
        ticket.setTicketCategory(testCategory);
        testEvent.decrementAvailableTickets();

        when(ticketRepository.findTicketWithEvent(ticket.getTicketId())).thenReturn(Optional.of(ticket));
        when(eventService.findEventEntityById(testEvent.getEventId())).thenReturn(testEvent);
        when(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).thenReturn(cache);

        ticketsService.deleteEmittedTicket(ticket.getTicketId());

        verify(ticketRepository).deleteById(ticket.getTicketId());
        verify(cache).evict(testEvent.getEventId());
        assertEquals(testEvent.getOriginalAmountOfTickets(), testEvent.getAvailableTickets());
    }

    @Test
    void deleteEmittedTicket_ShouldThrowResponseStatusException_WhenTicketNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findTicketWithEvent(id)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> ticketsService.deleteEmittedTicket(id));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(ticketRepository, never()).deleteById(any());
    }

    @Test
    void listAllTickets_ShouldReturnPaginatedTicketsDto_WhenCalledWithValidParameters() {
        int page = 0, pageSize = 10;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.DESC, "ticketId");

        List<Ticket> tickets = List.of(mockTicket(), mockTicket());
        Page<Ticket> ticketPage = new PageImpl<>(tickets, pageRequest, tickets.size());

        when(ticketRepository.findAllWithAssociations(pageRequest)).thenReturn(ticketPage);

        TicketsDto result = ticketsService.listAllTickets(page, pageSize);

        assertNotNull(result);
        assertEquals(tickets.size(), result.tickets().size());
    }

    @Test
    void listAllTickets_ShouldReturnEmptyTicketsDto_WhenNoTicketsExist() {
        int page = 0, pageSize = 10;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.DESC, "ticketId");
        Page<Ticket> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        when(ticketRepository.findAllWithAssociations(pageRequest)).thenReturn(emptyPage);

        TicketsDto result = ticketsService.listAllTickets(page, pageSize);

        assertNotNull(result);
        assertTrue(result.tickets().isEmpty());
    }

    @Test
    void listAllUserTickets_ShouldReturnPaginatedTicketsDtoForAuthenticatedUser_WhenCalled() {
        int page = 0, pageSize = 5;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketId");

        List<Ticket> userTickets = List.of(mockTicketWithUser(testUserId), mockTicketWithUser(testUserId));
        Page<Ticket> ticketPage = new PageImpl<>(userTickets, pageRequest, userTickets.size());

        when(jwtUtils.getAuthenticatedUsername()).thenReturn(testUsername);
        when(userService.findUserEntityByUserName(testUsername)).thenReturn(testUser);
        when(ticketRepository.findAllTicketsByUserId(testUserId, pageRequest)).thenReturn(ticketPage);

        TicketsDto result = ticketsService.listAllUserTickets(page, pageSize);

        assertNotNull(result);
        assertEquals(userTickets.size(), result.tickets().size());
    }

    @Test
    void listAllUserTickets_ShouldReturnEmptyTicketsDto_WhenUserHasNoTickets() {
        int page = 0, pageSize = 5;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketId");
        Page<Ticket> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        when(jwtUtils.getAuthenticatedUsername()).thenReturn(testUsername);
        when(userService.findUserEntityByUserName(testUsername)).thenReturn(testUser);
        when(ticketRepository.findAllTicketsByUserId(testUserId, pageRequest)).thenReturn(emptyPage);

        TicketsDto result = ticketsService.listAllUserTickets(page, pageSize);

        assertNotNull(result);
        assertTrue(result.tickets().isEmpty());
    }

    @Test
    void getAvailableTicketsByCategoryFromEvent_ShouldReturnListOfRemainingTicketCategories_WhenEventExists() {
        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);

        List<RemainingTicketCategoryDto> result = ticketsService.getAvailableTicketsByCategoryFromEvent(testEventId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(CATEGORY_NAME, result.getFirst().categoryName());
        assertEquals(300, result.getFirst().remainingTickets());
    }

    @Test
    void getAvailableTicketsByCategoryFromEvent_ShouldReturnEmptyList_WhenEventHasNoCategories() {
        testEvent.setTicketCategories(List.of());
        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);

        List<RemainingTicketCategoryDto> result = ticketsService.getAvailableTicketsByCategoryFromEvent(testEventId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private Ticket mockTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setTicketOwner(new User());
        ticket.setEvent(new Event());
        ticket.setTicketCategory(new TicketCategory());
        return ticket;
    }

    private Ticket mockTicketWithUser(UUID userId) {
        User user = new User();
        user.setUserId(userId);

        Ticket ticket = new Ticket();
        ticket.setTicketOwner(user);
        ticket.setTicketCategory(new TicketCategory());
        ticket.setEvent(new Event());
        return ticket;
    }
}
