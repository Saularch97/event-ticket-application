package com.example.booking.entities.dto;

import java.time.LocalDate;

public record CreateEventDto(
        String eventName,
        String eventDate,
        int eventHour,
        int eventMinute,
        String eventLocation,
        double eventPrice
) {
}
