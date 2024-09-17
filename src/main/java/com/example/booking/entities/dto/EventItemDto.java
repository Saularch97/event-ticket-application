package com.example.booking.entities.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventItemDto(UUID eventId, String eventName, LocalDateTime eventDate, int eventHour, int eventMinute, double eventPrice) {
}
