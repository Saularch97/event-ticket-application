package com.example.booking.controller.response;

import com.example.booking.controller.dto.TicketItemDto;

import java.util.List;

public record TicketsResponse(List<TicketItemDto> tickets,
                              int page,
                              int pageSize,
                              int totalPages,
                              long totalElements) {
}
