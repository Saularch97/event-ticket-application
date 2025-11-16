package com.example.booking.services;

import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.repositories.TicketCategoryRepository;
import com.example.booking.services.intefaces.TicketCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketCategoryServiceImpl implements TicketCategoryService {

    private static final Logger log = LoggerFactory.getLogger(TicketCategoryServiceImpl.class);

    private final TicketCategoryRepository repository;

    public TicketCategoryServiceImpl(TicketCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public List<TicketCategory> createTicketCategoriesForEvent(Event event, List<CreateTicketCategoryRequest> requests) {
        log.info("Creating {} ticket categories for event with ID {}", requests.size(), event.getEventId());

        List<TicketCategory> ticketCategories = new ArrayList<>();
        for (var req : requests) {
            log.debug("Creating ticket category with name '{}' and price {} for event ID {}", req.name(), req.price(), event.getEventId());

            var ticketCategory = createTicketCategory(req, event);
            ticketCategories.add(ticketCategory);
        }

        log.info("Created {} ticket categories for event with ID {}", ticketCategories.size(), event.getEventId());
        return ticketCategories;
    }

    // TODO implement new endpoint
    @Override
    public void addTicketCategoryToEvent( Event eventId) {
        return;
    }

    /*
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @ticketCategorySecurity.isEventOwner(#event.eventOwner.userId)")
    public void addTicketCategoryToEvent(AddTicketCategoryRequest request, UUID eventId) {
        log.info("Creating single ticket category '{}' for event with ID {}", request.name(), eventId);

        var event = eventsService.findEventEntityById(eventId);
    }
     */

    private TicketCategory createTicketCategory(CreateTicketCategoryRequest request, Event event) {
        log.info("Creating single ticket category '{}' for event with ID {}", request.name(), event.getEventId());

        var ticketCategory = new TicketCategory();
        ticketCategory.setEvent(event);
        ticketCategory.setAvailableCategoryTickets(request.availableCategoryTickets());
        ticketCategory.setPrice(request.price());
        ticketCategory.setName(request.name());

        var savedCategory = repository.save(ticketCategory);
        log.info("Ticket category '{}' created with ID {}", savedCategory.getName(), savedCategory.getTicketCategoryId());

        return savedCategory;
    }
}
