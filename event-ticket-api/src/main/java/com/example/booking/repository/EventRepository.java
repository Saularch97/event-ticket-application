package com.example.booking.repository;

import com.example.booking.domain.entities.Event;
import com.example.booking.dto.EventSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT e FROM Event e WHERE e.eventOwner.userId = :id")
    Page<Event> findAllEventsByUserId(@Param("id") UUID name, Pageable pageable);

    @Query("""
    SELECT new com.example.booking.dto.EventSummaryDto(
        e.eventId, e.eventName, e.eventLocation, e.availableTickets, e.eventDate
    )
    FROM Event e
    WHERE e.eventOwner.userId = :ownerId
    AND e.availableTickets > 0
    """)
    Page<EventSummaryDto> findAvailableEventsByOwner(UUID ownerId, Pageable pageable);
}
