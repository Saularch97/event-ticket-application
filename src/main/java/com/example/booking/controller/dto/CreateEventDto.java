package com.example.booking.controller.dto;

import java.time.LocalDate;

public record CreateEventDto(String eventName, String eventDate, int eventHour, int eventMinute, String eventLocation) {
}
