package com.example.booking.repositories;

import com.example.booking.domain.entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Payment findByUserEmail(String userEmail);
}