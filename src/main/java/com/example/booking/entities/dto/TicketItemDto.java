package com.example.booking.entities.dto;

import java.util.UUID;

public record TicketItemDto(UUID ticketId, EventItemDto eventItem, UserDto userDto) {
}
