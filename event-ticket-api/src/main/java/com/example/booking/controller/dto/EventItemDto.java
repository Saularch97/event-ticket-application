package com.example.booking.controller.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record EventItemDto(
        UUID eventId,
        String eventName,
        String eventDate,
        Integer eventHour,
        Integer eventMinute,
        Double eventPrice,
        Integer availableTickets,
        List<TicketCategoryDto> ticketCategories
) implements Serializable {
}
