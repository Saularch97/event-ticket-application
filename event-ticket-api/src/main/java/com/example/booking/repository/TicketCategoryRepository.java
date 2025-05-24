package com.example.booking.repository;

import com.example.booking.domain.entities.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
}
