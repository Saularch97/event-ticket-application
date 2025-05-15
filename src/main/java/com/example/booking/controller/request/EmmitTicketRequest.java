package com.example.booking.controller.request;

import java.util.UUID;

public record EmmitTicketRequest(UUID eventId, String ticketCategoryName) {
}
