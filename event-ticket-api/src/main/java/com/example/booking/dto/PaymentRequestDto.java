package com.example.booking.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentRequestDto(
        UUID orderId,
        BigDecimal totalAmount,
        List<TicketItemDto> items,
        String userEmail
) {}

