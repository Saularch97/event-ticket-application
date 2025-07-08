package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.dto.RemainingTicketCategoryDto;
import com.example.booking.dto.TicketItemDto;
import com.example.booking.dto.TicketsDto;
import com.example.booking.controller.request.ticket.EmmitTicketRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.repositories.TicketRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.TicketService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final EventsService eventService;
    private final JwtUtils jwtUtils;
    private final CacheManager cacheManager;

    public TicketServiceImpl(TicketRepository ticketRepository, UserService userService, EventsService eventService, JwtUtils jwtUtils, CacheManager cacheManager) {
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.eventService = eventService;
        this.jwtUtils = jwtUtils;
        this.cacheManager = cacheManager;
    }

    @Override
    public TicketItemDto emmitTicket(EmmitTicketRequest request) {

        String userName = jwtUtils.getAuthenticatedUsername();

        User user = userService.findUserEntityByUserName(userName);

        Event event = eventService.findEventEntityById(request.eventId());

        event.decrementAvailableTickets();

        Ticket ticket = new Ticket();
        ticket.setTicketOwner(user);
        ticket.setEvent(event);

        TicketCategory category = event.getTicketCategories()
                .stream()
                .filter(tc -> tc.getTicketCategoryId().equals(request.ticketCategoryId()))
                .findFirst().orElseThrow(() ->
                new IllegalArgumentException("Ticket category not found for id: " + request.ticketCategoryId())
        );

        if (category.getAvailableCategoryTickets() == 0) {
            throw new IllegalStateException("Category sold out!");
        }

        category.decrementTicketCategory();
        ticket.setTicketCategory(category);
        ticket.setTicketCategoryName(category.getName());
        ticket.setTicketPrice(category.getPrice());
        ticket.setTicketEventLocation(event.getEventLocation());
        ticket.setTicketEventDate(event.getEventDate().toString());

        ticketRepository.save(ticket);

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(event.getEventId());

        return Ticket.toTicketItemDto(ticket);
    }

    @Override
    public void deleteEmittedTicket(UUID ticketId) {
        var ticket = ticketRepository.findTicketWithEvent(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var event = eventService.findEventEntityById(ticket.getEvent().getEventId());

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(ticket.getEvent().getEventId());

        ticket.getTicketCategory().incrementTicketCategory();
        event.incrementAvailableTickets();

        ticketRepository.deleteById(ticketId);
    }

    @Override
    public TicketsDto listAllTickets(int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.DESC, "ticketId");

        Page<TicketItemDto> ticketsPage = ticketRepository
                .findAllWithAssociations(pageRequest)
                .map(Ticket::toTicketItemDto);

        return new TicketsDto(
                ticketsPage.getContent(),
                page,
                pageSize,
                ticketsPage.getTotalPages(),
                ticketsPage.getTotalElements()
        );
    }


    @Override
    public TicketsDto listAllUserTickets(int page, int pageSize) {
        String userName = jwtUtils.getAuthenticatedUsername();

        User user = userService.findUserEntityByUserName(userName);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketId");

        var tickets = ticketRepository
                .findAllTicketsByUserId(user.getUserId(), pageRequest)
                .map(Ticket::toTicketItemDto);

        return new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements());
    }

    @Cacheable(value = CacheNames.REMAINING_TICKETS, key = "#eventId")
    @Override
    public List<RemainingTicketCategoryDto> getAvailableTicketsByCategoryFromEvent(UUID eventId) {
        var event = eventService.findEventEntityById(eventId);

        return event.getTicketCategories().stream().map(ticketCategory ->
                new RemainingTicketCategoryDto(ticketCategory.getName(),
                ticketCategory.getAvailableCategoryTickets())
        ).toList();
    }

    @Override
    public TicketsDto getTicketsByCategoryId(Integer categoryId, int page, int pageSize) {

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticket_id");

        var tickets = ticketRepository
                .findTicketsByCategoryId(categoryId, pageRequest)
                .map(Ticket::toTicketItemDto);

        return new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements());
    }

    @Override
    public List<Ticket> findTicketsWithEventDetails(List<UUID> ticketIds) {
        return ticketRepository.findAllByIdWithEvent(ticketIds);
    }
}
