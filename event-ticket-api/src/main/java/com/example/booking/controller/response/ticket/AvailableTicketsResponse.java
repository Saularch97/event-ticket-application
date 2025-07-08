package com.example.booking.controller.response.ticket;

import com.example.booking.dto.RemainingTicketCategoryDto;

import java.util.List;

public record AvailableTicketsResponse(List<RemainingTicketCategoryDto> availableTickets) {
}
