package com.example.booking.controller.dto;

import java.util.UUID;

public record OrderTicketDto(
        String ticketName,
        Double ticketPrice,
        String ticketDate,
        UUID eventId,
        Integer eventHour,
        String eventLocation,
        Integer eventMinute
) {
}
