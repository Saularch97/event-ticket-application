package com.example.booking.controller.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(UUID eventId, String eventName, LocalDateTime eventDate, int eventHour, int eventMinute, double eventPrice) {
}
