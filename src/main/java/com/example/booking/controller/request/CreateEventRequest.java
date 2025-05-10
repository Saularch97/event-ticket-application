package com.example.booking.controller.request;

public record CreateEventRequest(
        String eventName,
        String eventDate,
        int eventHour,
        int eventMinute,
        String eventLocation,
        double eventPrice
) {
}
