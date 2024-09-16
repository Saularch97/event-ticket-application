package com.example.booking.controller.dto;

import java.util.UUID;

public record OrderTicketDto(
        UUID eventId,
        Double ticketPrice
) {
}
