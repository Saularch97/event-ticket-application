package com.example.booking.controller.dto;

import java.util.UUID;

public record TicketItemDto(UUID ticketId, UUID eventId, UUID userId, Integer ticketCategoryId) {
}
