package com.example.booking.repository;

import com.example.booking.entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends ListCrudRepository<Ticket, UUID> {
}
