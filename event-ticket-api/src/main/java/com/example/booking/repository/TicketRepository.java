package com.example.booking.repository;

import com.example.booking.domain.entities.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("SELECT t FROM Ticket t WHERE t.ticketOwner.userId = :id")
    Page<Ticket> findAllTicketsByUserId(@Param("id") UUID id, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.emittedAt > :timestamp")
    Page<Ticket> findTicketsEmittedAfter(@Param("timestamp") LocalDateTime timestamp, Pageable pageable);

    @Query(value = """
    SELECT t.*
    FROM tb_tickets t
    INNER JOIN tb_ticket_category tc
        ON t.ticket_category_id = tc.ticket_category_id
    WHERE tc.ticket_category_id = ?1
    """,
    countQuery = """
    SELECT count(*)
    FROM tb_tickets t
    INNER JOIN tb_ticket_category tc
        ON t.ticket_category_id = tc.ticket_category_id
    WHERE tc.ticket_category_id = ?1
    """,
    nativeQuery = true)
    Page<Ticket> findTicketsByCategoryId(Integer categoryId, Pageable pageable);

    @Query("""
    SELECT t FROM Ticket t
    JOIN FETCH t.event
    WHERE t.ticketId = :ticketId
    """)
    Optional<Ticket> findTicketWithEvent(@Param("ticketId") UUID ticketId);

    @Query(value = """
    SELECT t FROM Ticket t
    JOIN FETCH t.event e
    JOIN FETCH t.ticketOwner o
    JOIN FETCH t.ticketCategory c
    """,
    countQuery = "SELECT count(t) FROM Ticket t")
    Page<Ticket> findAllWithAssociations(Pageable pageable);
}
