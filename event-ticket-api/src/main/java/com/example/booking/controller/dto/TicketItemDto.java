package com.example.booking.controller.dto;

import java.util.UUID;

public record TicketItemDto(UUID ticketId, EventItemDto eventItem, UserDto userDto, TicketCategoryDto ticketCategoryDto) {
}
