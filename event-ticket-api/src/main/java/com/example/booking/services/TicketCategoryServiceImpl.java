package com.example.booking.services;

import com.example.booking.dto.TicketCategoryDto;
import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.repositories.TicketCategoryRepository;
import com.example.booking.services.intefaces.TicketCategoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private final TicketCategoryRepository repository;

    public TicketCategoryServiceImpl(TicketCategoryRepository repository) {
        this.repository = repository;
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

    public TicketCategoryDto createTicketCategory(CreateTicketCategoryRequest request, Event event) {

        var ticketCategory = new TicketCategory();
        ticketCategory.setEvent(event);
        ticketCategory.setAvailableCategoryTickets(request.availableCategoryTickets());
        ticketCategory.setPrice(request.price());
        ticketCategory.setName(request.name());

        return TicketCategory.toTicketCategoryDto(repository.save(ticketCategory));
    }
}
