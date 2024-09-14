package com.example.booking.repository;

import com.example.booking.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketOrderRepository extends JpaRepository<Order, UUID> {
}
