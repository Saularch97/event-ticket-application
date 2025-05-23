package com.example.booking.controller.response;

import com.example.booking.controller.dto.RemainingTicketCategoryDto;

import java.util.List;

public record AvailableTicketsResponse(List<RemainingTicketCategoryDto> availableTickets) {
}
