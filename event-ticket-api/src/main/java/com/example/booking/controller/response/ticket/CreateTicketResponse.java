package com.example.booking.controller.response.ticket;

import java.util.UUID;

public record CreateTicketResponse(UUID ticketId, UUID eventId, UUID userId, Long ticketCategoryId) {
}
