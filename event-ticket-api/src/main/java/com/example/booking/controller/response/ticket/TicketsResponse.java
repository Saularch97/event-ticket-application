package com.example.booking.controller.response.ticket;

import com.example.booking.dto.TicketItemDto;

import java.util.List;

public record TicketsResponse(List<TicketItemDto> tickets,
                              int page,
                              int pageSize,
                              int totalPages,
                              long totalElements) {
}
