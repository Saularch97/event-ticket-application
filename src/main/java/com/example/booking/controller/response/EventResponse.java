package com.example.booking.controller.response;

import java.util.UUID;

public record EventResponse(UUID eventId, String eventName, String eventDate, int eventHour, int eventMinute, double eventPrice) {
}
