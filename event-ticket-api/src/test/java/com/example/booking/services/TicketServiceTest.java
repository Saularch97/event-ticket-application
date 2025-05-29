package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    CacheManager cacheManager;

    @InjectMocks
    TicketsServiceImpl ticketsService;

    // Compartilhados nos dois testes
    private EmmitTicketRequest emmitTicketRequest;
    private Event mockedEvent;
    private User mockedUser;

    // emmitTicket_shouldThrowException_whenEventNotFound

    // emmitTicket_shouldThrowException_whenCategoryNotFound

    // deleteEmittedTicket_shouldDeleteTicket_whenTicketExists

    // deleteEmittedTicket_shouldThrowException_whenTicketNotFound

    // deleteEmittedTicket_shouldThrowException_whenEventNotFound

    // listAllTickets_shouldReturnTicketsDto_whenTicketsExist

    // listAllUserTickets_shouldReturnUserTickets_whenUserExists

    // listAllUserTickets_shouldThrowException_whenUserNotFound

    // getAvailableTicketsByCategoryFromEvent_shouldReturnCategoryList_whenEventExists

    // getAvailableTicketsByCategoryFromEvent_shouldThrowException_whenEventNotFound

    @BeforeEach
    void setUp() {
        emmitTicketRequest = new EmmitTicketRequest(UUID.randomUUID(), "Ingresso maneiro");

        mockedEvent = mock(Event.class);
        mockedUser = mock(User.class);

        when(jwtUtils.getAuthenticatedUsername()).thenReturn("adminUser");
    }

    @Test
    void emmitTicket_WhenEmmitTicketRequestIsReceived_ShouldEmmitAnTicketCorrectly() {
        Optional<Event> optionalMockedEvent = Optional.of(mockedEvent);
        Optional<User> optionalUser = Optional.of(mockedUser);

        TicketCategory category = new TicketCategory();
        category.setName("Ingresso maneiro");
        category.setAvailableCategoryTickets(10);

        Cache mockedCache = mock(Cache.class);

        when(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).thenReturn(mockedCache);
        when(mockedEvent.getTicketCategories()).thenReturn(List.of(category));
        when(mockedEvent.getEventDate()).thenReturn(LocalDateTime.now());
        when(userRepository.findByUserName("adminUser")).thenReturn(optionalUser);
        when(eventRepository.findById(any(UUID.class))).thenReturn(optionalMockedEvent);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketItemDto ticketItemDto = ticketsService.emmitTicket(emmitTicketRequest);

        assertEquals("Ingresso maneiro", ticketItemDto.ticketCategoryDto().name());
        assertNotNull(ticketItemDto);
    }

    @Test
    void emmitTicket_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByUserName("adminUser")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                ticketsService.emmitTicket(emmitTicketRequest)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getReason()).isEqualTo("User not found!");
    }
}
