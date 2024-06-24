package com.example.booking.controller.dto;


import java.util.List;

public record TicketsDto(List<TicketItemDto> tickets,
                         int page,
                         int pageSize,
                         int totalPages,
                         long totalElements) {
}
