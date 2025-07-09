package com.example.booking.repositories;

import com.example.booking.domain.entities.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;


public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    @Query(value = "SELECT * FROM tb_ticket_category WHERE event_id = ?1", nativeQuery = true)
    List<TicketCategory> findAllTicketCategoriesByEventId(UUID eventId);
}
