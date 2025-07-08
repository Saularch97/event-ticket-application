package com.example.booking.services.scheduler;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.domain.entities.Event;
import com.example.booking.repositories.EventRepository;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
public class EventUpdater {

    private final EventRepository eventRepository;

    public EventUpdater(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Scheduled(fixedRate = 300000)
    @CacheEvict(value = CacheNames.TOP_EVENTS, key = "'topTrending'")
    @Transactional
    public void updateEventStatuses() {

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        List<Event> events = eventRepository.findAll();

        events.forEach(event -> {
           Long ticketsEmittedOneOurAgo = event.getTickets().stream().filter(ticket -> ticket.getEmittedAt().isAfter(oneHourAgo)).count();
           event.setTicketsEmittedInTrendingPeriod(ticketsEmittedOneOurAgo);
           event.setTrending(false);
        });

        List<Event> topThreeTrendingEvents = events.stream().sorted(Comparator.comparingLong(Event::getTicketsEmittedInTrendingPeriod).reversed()).limit(3).toList();

        topThreeTrendingEvents.forEach(event -> {
            event.setTrending(true);
        });

        eventRepository.saveAll(events);
    }
}
