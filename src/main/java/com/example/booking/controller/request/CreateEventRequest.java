package com.example.booking.controller.request;

import java.util.List;

public record CreateEventRequest(
        String eventName,
        String eventDate,
        Integer eventHour,
        Integer eventMinute,
        String eventLocation,
        Double eventPrice,
        Integer availableTickets,
        List<CreateTicketCategoryRequest> ticketCategories
) {
}
