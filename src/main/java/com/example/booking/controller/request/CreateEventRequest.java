package com.example.booking.controller.request;

public record CreateEventRequest(
        String eventName,
        String eventDate,
        Integer eventHour,
        Integer eventMinute,
        String eventLocation,
        Double eventPrice,
        Integer availableTickets
) {
}
