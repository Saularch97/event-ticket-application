package com.example.booking.entities.dto;

import java.util.UUID;

// TODO tirar ticketPrice
public record OrderTicketDto(
        UUID eventId,
        Double ticketPrice
) {
}
