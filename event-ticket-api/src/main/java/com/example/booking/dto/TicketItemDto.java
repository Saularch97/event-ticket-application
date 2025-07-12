package com.example.booking.dto;

import java.io.Serializable;
import java.util.UUID;

public record TicketItemDto(
        UUID ticketId,
        UUID eventId,
        UUID userId,
        Long ticketCategoryId
) implements Serializable {
}
