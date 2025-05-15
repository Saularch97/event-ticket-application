package com.example.booking.controller.dto;

import com.example.booking.controller.response.TicketCategoryResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EventItemDto(UUID eventId, String eventName, String eventDate, Integer eventHour, Integer eventMinute, Double eventPrice, Integer availableTickets, List<TicketCategoryDto> ticketCategories) {
}
