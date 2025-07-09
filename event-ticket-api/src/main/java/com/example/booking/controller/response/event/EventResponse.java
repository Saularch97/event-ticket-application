package com.example.booking.controller.response.event;

import java.util.UUID;

public record EventResponse(UUID eventId, String eventName, String eventDate, Integer eventHour, Integer eventMinute, Integer availableTickets) {
}
