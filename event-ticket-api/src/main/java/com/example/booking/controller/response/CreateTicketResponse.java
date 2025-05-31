package com.example.booking.controller.response;

import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.TicketCategoryDto;
import com.example.booking.controller.dto.UserDto;

import java.util.UUID;

public record CreateTicketResponse(UUID ticketId, UUID eventId, UUID userId, Integer ticketCategoryId) {
}
