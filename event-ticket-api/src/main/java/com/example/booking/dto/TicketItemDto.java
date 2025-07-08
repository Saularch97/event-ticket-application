package com.example.booking.dto;

import java.util.UUID;

public record TicketItemDto(
        UUID ticketId,
        UUID eventId,
        UUID userId,
        Integer ticketCategoryId
) {
}
