package com.example.booking.services.intefaces;

import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;

import java.util.List;
import java.util.UUID;

public interface TicketCategoryService {

    List<TicketCategory> createTicketCategoriesForEvent(Event event, List<CreateTicketCategoryRequest> requests);

    void addTicketCategoryToEvent(Event eventId);
}
