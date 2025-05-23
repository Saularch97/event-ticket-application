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
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("SELECT t FROM Ticket t WHERE t.ticketOwner.userId = :id")
    Page<Ticket> findAllTicketsByUserId(@Param("id") UUID id, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.emittedAt > :timestamp")
    List<Ticket> findTicketsEmittedAfter(@Param("timestamp") LocalDateTime timestamp);
}
