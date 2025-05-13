package com.example.booking.controller.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventItemDto(UUID eventId, String eventName, String eventDate, Integer eventHour, Integer eventMinute, Double eventPrice, Integer availableTickets) {
}
