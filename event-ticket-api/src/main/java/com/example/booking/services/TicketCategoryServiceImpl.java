package com.example.booking.services;

import com.example.booking.controller.dto.TicketCategoryDto;
import com.example.booking.controller.request.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.repository.TicketCategoryRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.TicketCategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TicketCategoryServiceImpl implements TicketCategoryService {

    private final TicketCategoryRepository repository;
    private final EventsService eventsService;

    public TicketCategoryServiceImpl(TicketCategoryRepository repository, EventsService eventsService) {
        this.repository = repository;
        this.eventsService = eventsService;
    }

    @Override
    public List<TicketCategory> createTicketCategoriesForEvent(Event event, List<CreateTicketCategoryRequest> requests) {
        List<TicketCategory> ticketCategories = new ArrayList<>();
        for (var req : requests) {
            TicketCategory tc = new TicketCategory();
            tc.setEvent(event);
            tc.setName(req.name());
            tc.setPrice(req.price());
            tc.setAvailableCategoryTickets(req.availableCategoryTickets());
            ticketCategories.add(tc);
        }
        return ticketCategories;
    }

    public TicketCategoryDto createTicketCategory(CreateTicketCategoryRequest request, UUID eventId) {
        var event = eventsService.findEventEntityById(eventId);

        var ticketCategory = new TicketCategory();
        ticketCategory.setEvent(event);
        ticketCategory.setAvailableCategoryTickets(request.availableCategoryTickets());
        ticketCategory.setPrice(request.price());
        ticketCategory.setName(request.name());

        return TicketCategory.toTicketCategoryDto(repository.save(ticketCategory));
    }
}
