package com.example.booking.services.scheduler;

import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventUpdaterTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventUpdater eventUpdater;

    @Captor
    private ArgumentCaptor<List<Event>> eventsCaptor;

    @Test
    void updateEventStatuses_ShouldMarkTopThreeEventsAsTrending_WhenExecuted() {
        LocalDateTime now = LocalDateTime.now();

        Set<Ticket> ticketsForEvent1 = Stream.generate(() -> mockTicket(now.minusMinutes(10))).limit(5).collect(Collectors.toSet());
        Set<Ticket> ticketsForEvent2 = Stream.generate(() -> mockTicket(now.minusMinutes(20))).limit(3).collect(Collectors.toSet());
        Set<Ticket> ticketsForEvent3 = Set.of(mockTicket(now.minusHours(2)), mockTicket(now.minusMinutes(5)));
        Set<Ticket> ticketsForEvent4 = Set.of(mockTicket(now.minusDays(1)));

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);
        Event event4 = mock(Event.class);

        when(event1.getTickets()).thenReturn(ticketsForEvent1);
        when(event2.getTickets()).thenReturn(ticketsForEvent2);
        when(event3.getTickets()).thenReturn(ticketsForEvent3);
        when(event4.getTickets()).thenReturn(ticketsForEvent4);

        List<Event> allEvents = List.of(event1, event2, event3, event4);
        when(eventRepository.findAll()).thenReturn(allEvents);

        eventUpdater.updateEventStatuses();

        verify(eventRepository, times(1)).saveAll(eventsCaptor.capture());

        List<Event> savedEvents = eventsCaptor.getValue();

        savedEvents.forEach(event -> {
            if (event == event1 || event == event2 || event == event3) {
                verify(event).setTrending(true);
            }
            if (event == event4) {
                verify(event).setTrending(false);
                verify(event, never()).setTrending(true);
            }
        });
    }

    private Ticket mockTicket(LocalDateTime emittedAt) {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getEmittedAt()).thenReturn(emittedAt);
        return ticket;
    }
}
