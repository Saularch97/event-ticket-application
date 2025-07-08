package com.example.booking.repositories;

import com.example.booking.domain.entities.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query(value = """
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.tickets t
    LEFT JOIN FETCH o.user
    LEFT JOIN FETCH t.event
    LEFT JOIN FETCH t.ticketCategory
    WHERE o.user.userId = :userId
    """,
    countQuery = "SELECT count(o) FROM Order o WHERE o.user.userId = :userId")
    Page<Order> findOrdersByUserIdWithAssociations(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
    SELECT o FROM Order o
    LEFT JOIN FETCH o.tickets t
    LEFT JOIN FETCH t.event e
    LEFT JOIN FETCH t.ticketCategory tc
    WHERE o.orderId = :orderId
    """)
    Optional<Order> findByIdWithFullAssociations(@Param("orderId") UUID orderId);
}
