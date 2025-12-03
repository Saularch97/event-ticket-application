package com.example.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestProducerDto(
        UUID orderId,
        BigDecimal amount,
        UUID userId,
        String userEmail,
        String description,
        String statementDescriptor
) {}
