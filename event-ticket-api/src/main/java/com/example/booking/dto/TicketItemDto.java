package com.example.booking.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

    public record TicketItemDto(
            UUID ticketId,
            UUID eventId,
            UUID userId,
            Long ticketCategoryId,
            BigDecimal price
) implements Serializable {
}
