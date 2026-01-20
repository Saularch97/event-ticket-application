package com.example.booking.repositories;

import com.example.booking.domain.entities.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;


public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    @Query(value = "SELECT * FROM tb_ticket_category WHERE event_id = ?1", nativeQuery = true)
    List<TicketCategory> findAllTicketCategoriesByEventId(UUID eventId);

    @Modifying
    @Query("UPDATE TicketCategory tc SET tc.availableCategoryTickets = tc.availableCategoryTickets - 1 " +
            "WHERE tc.ticketCategoryId = :id AND tc.availableCategoryTickets > 0")
    int decrementQuantity(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TicketCategory tc SET tc.availableCategoryTickets = tc.availableCategoryTickets + 1 WHERE tc.ticketCategoryId = :id")
    void incrementQuantity(@Param("id") Long id);
}
