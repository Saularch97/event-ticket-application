package com.example.booking.repositories;

import com.example.booking.domain.entities.Event;
import com.example.booking.dto.EventSummaryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CustomEventRepositoryTest  {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldReturnOnlyEventsAfterStartDateAndMatchingLocation() {
        LocalDateTime now = LocalDateTime.now();
        Event oldEvent = new Event();
        oldEvent.setEventName("Show Antigo");
        oldEvent.setEventLocation("Alfenas");
        oldEvent.setEventDate(now.minusDays(5));
        oldEvent.setAvailableTickets(10);
        testEntityManager.persist(oldEvent);

        Event futureEvent = new Event();
        futureEvent.setEventName("Show Futuro");
        futureEvent.setEventLocation("Alfenas");
        futureEvent.setEventDate(now.plusDays(1));
        futureEvent.setAvailableTickets(10);
        testEntityManager.persist(futureEvent);
        testEntityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);
        Page<EventSummaryDto> result = eventRepository.findByCriteria(
                "Show", "Alfenas", now, null, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertEquals("Show Futuro", result.getContent().getFirst().name());
    }
}