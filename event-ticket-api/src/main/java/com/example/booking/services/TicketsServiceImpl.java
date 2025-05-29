package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.dto.RemainingTicketCategoryDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.TicketsService;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TicketsServiceImpl implements TicketsService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final JwtUtils jwtUtils;
    private final CacheManager cacheManager;

    public TicketsServiceImpl(TicketRepository ticketRepository, UserRepository userRepository, EventRepository eventRepository, JwtUtils jwtUtils, CacheManager cacheManager) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.jwtUtils = jwtUtils;
        this.cacheManager = cacheManager;
    }

    public TicketItemDto emmitTicket(EmmitTicketRequest request) {

        String userName = jwtUtils.getAuthenticatedUsername();

        User user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found!"));

        event.decrementAvailableTickets();

        Ticket ticket = new Ticket();
        ticket.setTicketOwner(user);
        ticket.setEvent(event);

        TicketCategory category = event.getTicketCategories()
                .stream()
                .filter(tc -> tc.getName().equalsIgnoreCase(request.ticketCategoryName()))
                .findFirst().orElseThrow(() ->
                new IllegalArgumentException("Category not found! " + request.ticketCategoryName())
        );

        category.decrementTicketCategory();
        ticket.setTicketCategory(category);

        ticketRepository.save(ticket);

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(event.getEventId());

        return Ticket.toTicketItemDto(ticket);
    }

    public void deleteEmittedTicket(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var event = eventRepository.findById(ticket.getEvent().getEventId()).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND)
        );

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(ticket.getEvent().getEventId());

        ticket.getTicketCategory().incrementTicketCategory();
        event.incrementAvailableTickets();

        ticketRepository.deleteById(ticketId);
    }

    public TicketsDto listAllTickets(int page, int pageSize) {
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.DESC, "ticketId");

        Page<TicketItemDto> ticketsPage = ticketRepository
                .findAll(pageRequest)
                .map(Ticket::toTicketItemDto);

        return new TicketsDto(
                ticketsPage.getContent(),
                page,
                pageSize,
                ticketsPage.getTotalPages(),
                ticketsPage.getTotalElements()
        );
    }


    public TicketsDto listAllUserTickets(int page, int pageSize) {
        String userName = jwtUtils.getAuthenticatedUsername();

        Optional<User> user = userRepository.findByUserName(userName);

        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketId");

        var tickets = ticketRepository
                .findAllTicketsByUserId(user.get().getUserId(), pageRequest)
                .map(Ticket::toTicketItemDto);

        return new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements());
    }

    @Cacheable(value = CacheNames.REMAINING_TICKETS, key = "#eventId")
    public List<RemainingTicketCategoryDto> getAvailableTicketsByCategoryFromEvent(UUID eventId) {
        var event = eventRepository.findById(eventId).orElseThrow(()
                -> new ResponseStatusException(HttpStatus.NOT_FOUND)
        );

        return event.getTicketCategories().stream().map(ticketCategory -> {
            return new RemainingTicketCategoryDto(ticketCategory.getName(), ticketCategory.getAvailableCategoryTickets());
        }).toList();
    }
}
