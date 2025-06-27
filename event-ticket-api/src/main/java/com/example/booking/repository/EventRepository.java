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

    @Query(value = """
            SELECT e.*
            FROM tb_events e
            INNER JOIN tb_users u ON e.user_id = u.user_id
            WHERE u.user_id = ?1
            AND e.available_tickets > 0
            """,
            countQuery = """
                    SELECT count(*)
                    FROM tb_events e
                    INNER JOIN tb_users u ON e.user_id = u.user_id
                    WHERE u.user_id = ?1
                    AND e.available_tickets > 0
                    """,
            nativeQuery = true)
    Page<Event> findAvailableEventsByOwner(UUID ownerId, Pageable pageable);

}
