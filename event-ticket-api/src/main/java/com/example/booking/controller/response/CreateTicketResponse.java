package com.example.booking.controller.response;

import java.util.UUID;

public record CreateTicketResponse(UUID ticketId, UUID eventId, UUID userId, Integer ticketCategoryId) {
}
