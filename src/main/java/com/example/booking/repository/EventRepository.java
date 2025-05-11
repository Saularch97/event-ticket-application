package com.example.booking.repository;

import com.example.booking.domain.entities.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT e FROM Event e WHERE e.eventOwner.userId = :id")
    Page<Event> findAllEventsByUserId(@Param("id") UUID name, Pageable pageable);
}
