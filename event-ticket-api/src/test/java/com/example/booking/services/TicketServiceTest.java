package com.example.booking.services;

import com.example.booking.builders.EventBuilder;
import com.example.booking.builders.TicketBuilder;
import com.example.booking.builders.TicketCategoryBuilder;
import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.request.ticket.EmmitTicketRequest;
import com.example.booking.domain.entities.*;
import com.example.booking.dto.RemainingTicketCategoryDto;
import com.example.booking.dto.TicketItemDto;
import com.example.booking.dto.TicketsDto;
import com.example.booking.exception.TicketCategoryNotFoundException;
import com.example.booking.exception.TicketCategorySoldOutException;
import com.example.booking.exception.TicketNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final String CATEGORY_NAME = "Pista Premium";
    private static final String TEST_LOCATION = "Estádio Nacional";
    private static final String TEST_USERNAME = "testUser";
    private static final Long TEST_TICKET_CATEGORY_ID = 1L;
    private static final Long INVALID_TICKET_CATEGORY_ID = 2L;
    private static final Double TEST_CATEGORY_PRICE = 250.0;
    private static final Integer TEST_CATEGORY_TICKETS = 300;
    private static final Integer TEST_EVENT_TICKETS = 500;
    private static final LocalDateTime TEST_EVENT_DATETIME = LocalDateTime.of(2025, 12, 15, 20, 0);
    private static final Integer SOLD_OUT_TICKETS = 0;
    private static final Integer PAGE_0 = 0;
    private static final Integer PAGE_SIZE_5 = 5;
    private static final Integer PAGE_SIZE_10 = 10;
    private static final String SORT_BY_TICKET_ID = "ticketId";
    private static final Integer EXPECTED_LIST_SIZE_1 = 1;

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserService userService;
    @Mock
    private EventsService eventService;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private TicketServiceImpl ticketsService;

    private User testUser;
    private Event testEvent;
    private TicketCategory testCategory;
    private UUID testEventId;
    private Long testTicketCategoryId;
    private UUID testUserId;
    private String testUsername;

    @BeforeEach
    void setUp() {
        testUsername = TEST_USERNAME;
        testUserId = UUID.randomUUID();
        testEventId = UUID.randomUUID();
        testTicketCategoryId = TEST_TICKET_CATEGORY_ID;

        testUser = new User();
        testUser.setUserId(testUserId);
        testUser.setUserName(testUsername);

        testCategory = TicketCategoryBuilder.aTicketCategory()
                .withTicketCategoryId(testTicketCategoryId)
                .withName(CATEGORY_NAME)
                .withPrice(TEST_CATEGORY_PRICE)
                .withAvailableCategoryTickets(TEST_CATEGORY_TICKETS)
                .build();

        testEvent = EventBuilder.anEvent()
                .withEventId(testEventId)
                .withEventLocation(TEST_LOCATION)
                .withEventDate(TEST_EVENT_DATETIME)
                .withTicketCategories(List.of(testCategory))
                .withAvailableTickets(TEST_EVENT_TICKETS)
                .build();
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
        assertEquals(TEST_EVENT_TICKETS - 1, testEvent.getAvailableTickets());
    }

    @Test
    void emmitTicket_ShouldTicketCategoryNotFoundException_WhenCategoryNotFound() {
        EmmitTicketRequest request = new EmmitTicketRequest(testEventId, INVALID_TICKET_CATEGORY_ID);

        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);

        assertThrows(TicketCategoryNotFoundException.class,
                () -> ticketsService.emmitTicket(request));

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void emmitTicket_ShouldThrowTicketSoldOutException_WhenNoTicketsAvailableInCategory() {
        testEvent.getTicketCategories().getFirst().setAvailableCategoryTickets(SOLD_OUT_TICKETS);
        EmmitTicketRequest request = new EmmitTicketRequest(testEventId, testTicketCategoryId);

        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);

        assertThrows(TicketCategorySoldOutException.class, () -> ticketsService.emmitTicket(request));

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void deleteEmittedTicket_ShouldDeleteTicketAndUpdateCounts_WhenTicketExists() {
        Ticket ticket = TicketBuilder.aTicket()
                .withTicketId(UUID.randomUUID())
                .withEvent(testEvent)
                .withTicketCategory(testCategory)
                .build();

        testEvent.decrementAvailableTickets();

        when(ticketRepository.findTicketWithEvent(ticket.getTicketId())).thenReturn(Optional.of(ticket));
        when(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).thenReturn(cache);

        ticketsService.deleteEmittedTicket(ticket.getTicketId());

        verify(ticketRepository).deleteById(ticket.getTicketId());
        verify(cache).evict(testEvent.getEventId());
        assertEquals(testEvent.getOriginalAmountOfTickets(), testEvent.getAvailableTickets());
    }

    @Test
    void deleteEmittedTicket_ShouldThrowTicketNotFoundException_WhenTicketNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findTicketWithEvent(id)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class,
                () -> ticketsService.deleteEmittedTicket(id));

        verify(ticketRepository, never()).deleteById(any());
    }

    @Test
    void listAllTickets_ShouldReturnPaginatedTicketsDto_WhenCalledWithValidParameters() {
        PageRequest pageRequest = PageRequest.of(PAGE_0, PAGE_SIZE_10, Sort.Direction.DESC, SORT_BY_TICKET_ID);

        List<Ticket> tickets = List.of(mockTicket(), mockTicket());
        Page<Ticket> ticketPage = new PageImpl<>(tickets, pageRequest, tickets.size());

        when(ticketRepository.findAllWithAssociations(pageRequest)).thenReturn(ticketPage);

        TicketsDto result = ticketsService.listAllTickets(PAGE_0, PAGE_SIZE_10);

        assertNotNull(result);
        assertEquals(tickets.size(), result.tickets().size());
    }

    @Test
    void listAllTickets_ShouldReturnEmptyTicketsDto_WhenNoTicketsExist() {
        PageRequest pageRequest = PageRequest.of(PAGE_0, PAGE_SIZE_10, Sort.Direction.DESC, SORT_BY_TICKET_ID);
        Page<Ticket> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        when(ticketRepository.findAllWithAssociations(pageRequest)).thenReturn(emptyPage);

        TicketsDto result = ticketsService.listAllTickets(PAGE_0, PAGE_SIZE_10);

        assertNotNull(result);
        assertTrue(result.tickets().isEmpty());
    }

    @Test
    void listAllUserTickets_ShouldReturnPaginatedTicketsDtoForAuthenticatedUser_WhenCalled() {
        PageRequest pageRequest = PageRequest.of(PAGE_0, PAGE_SIZE_5, Sort.Direction.ASC, SORT_BY_TICKET_ID);

        List<Ticket> userTickets = List.of(mockTicketWithUser(testUserId), mockTicketWithUser(testUserId));
        Page<Ticket> ticketPage = new PageImpl<>(userTickets, pageRequest, userTickets.size());

        when(jwtUtils.getAuthenticatedUsername()).thenReturn(testUsername);
        when(userService.findUserEntityByUserName(testUsername)).thenReturn(testUser);
        when(ticketRepository.findAllTicketsByUserId(testUserId, pageRequest)).thenReturn(ticketPage);

        TicketsDto result = ticketsService.listAllUserTickets(PAGE_0, PAGE_SIZE_5);

        assertNotNull(result);
        assertEquals(userTickets.size(), result.tickets().size());
    }

    @Test
    void listAllUserTickets_ShouldReturnEmptyTicketsDto_WhenUserHasNoTickets() {
        PageRequest pageRequest = PageRequest.of(PAGE_0, PAGE_SIZE_5, Sort.Direction.ASC, SORT_BY_TICKET_ID);
        Page<Ticket> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        when(jwtUtils.getAuthenticatedUsername()).thenReturn(testUsername);
        when(userService.findUserEntityByUserName(testUsername)).thenReturn(testUser);
        when(ticketRepository.findAllTicketsByUserId(testUserId, pageRequest)).thenReturn(emptyPage);

        TicketsDto result = ticketsService.listAllUserTickets(PAGE_0, PAGE_SIZE_5);

        assertNotNull(result);
        assertTrue(result.tickets().isEmpty());
    }

    @Test
    void getAvailableTicketsByCategoryFromEvent_ShouldReturnListOfRemainingTicketCategories_WhenEventExists() {
        when(eventService.findEventEntityById(testEventId)).thenReturn(testEvent);

        List<RemainingTicketCategoryDto> result = ticketsService.getAvailableTicketsByCategoryFromEvent(testEventId);

        assertNotNull(result);
        assertEquals(EXPECTED_LIST_SIZE_1, result.size());
        assertEquals(CATEGORY_NAME, result.getFirst().categoryName());
        assertEquals(TEST_CATEGORY_TICKETS, result.getFirst().remainingTickets());
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
        return TicketBuilder.aTicket()
                .withTicketId(UUID.randomUUID())
                .withTicketOwner(new User())
                .withEvent(new Event())
                .withTicketCategory(new TicketCategory())
                .build();
    }

    private Ticket mockTicketWithUser(UUID userId) {
        User user = new User();
        user.setUserId(userId);

        return TicketBuilder.aTicket()
                .withTicketOwner(user)
                .withTicketCategory(new TicketCategory())
                .withEvent(new Event())
                .build();
    }
}