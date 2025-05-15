package com.example.booking.repository;

import com.example.booking.domain.entities.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {
}
