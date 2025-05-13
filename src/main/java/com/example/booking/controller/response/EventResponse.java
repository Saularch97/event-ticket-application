package com.example.booking.controller.response;

import java.util.UUID;

public record EventResponse(UUID eventId, String eventName, String eventDate, Integer eventHour, Integer eventMinute, Double eventPrice, Integer availableTickets) {
}
