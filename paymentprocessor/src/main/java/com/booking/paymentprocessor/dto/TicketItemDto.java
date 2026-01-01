package com.booking.paymentprocessor.dto;

import java.math.BigDecimal;

public record TicketItemDto(
        String categoryName,
        BigDecimal price,
        String ticketId
) {}
