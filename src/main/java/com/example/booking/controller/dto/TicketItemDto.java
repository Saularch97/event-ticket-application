package com.example.booking.controller.dto;

import java.util.UUID;

public record TicketItemDto(UUID ticketId, String ticketName, Double ticketPrice, String ticketDate, Integer ticketHour, Integer ticketMinute) {
}
