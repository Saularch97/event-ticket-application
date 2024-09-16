package com.example.booking.controller.dto;

import java.util.UUID;

public record TicketItemDto(UUID ticketId, EventItemDto eventItem, Double ticketPrice, UserDto userDto) {
}
