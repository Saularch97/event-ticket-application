package com.example.booking.services.intefaces;

import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;

import java.util.List;

public interface TicketCategoryService {

    public List<TicketCategory> createTicketCategoriesForEvent(Event event, List<CreateTicketCategoryRequest> requests);
}
