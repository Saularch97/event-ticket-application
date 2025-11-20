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
import com.example.booking.exception.TicketCategoryNotFoundException;
import com.example.booking.exception.TicketCategorySoldOutException;
import com.example.booking.exception.TicketNotFoundException;
import com.example.booking.repositories.TicketRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.TicketService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

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
    @PreAuthorize("isAuthenticated()")
    public TicketItemDto emmitTicket(EmmitTicketRequest request) {
        String userName = jwtUtils.getAuthenticatedUsername();
        log.info("User '{}' requested to emit ticket for eventId={}, ticketCategoryId={}", userName, request.eventId(), request.ticketCategoryId());

        User user = userService.findUserEntityByUserName(userName);
        Event event = eventService.findEventEntityById(request.eventId());

        event.decrementAvailableTickets();

        Ticket ticket = new Ticket();
        ticket.setTicketOwner(user);
        ticket.setEvent(event);

        TicketCategory category = event.getTicketCategories()
                .stream()
                .filter(tc -> tc.getTicketCategoryId().equals(request.ticketCategoryId()))
                .findFirst().orElseThrow(() -> {
                    log.warn("Ticket category with id {} not found for event {}", request.ticketCategoryId(), event.getEventId());
                    return new TicketCategoryNotFoundException(request.ticketCategoryId());
                });

        if (category.getAvailableCategoryTickets() == 0) {
            log.warn("Ticket category '{}' sold out for event {}", category.getName(), event.getEventId());
            throw new TicketCategorySoldOutException();
        }

        category.decrementTicketCategory();
        ticket.setTicketCategory(category);
        ticket.setTicketCategoryName(category.getName());
        ticket.setTicketPrice(category.getPrice());
        ticket.setTicketEventLocation(event.getEventLocation());
        ticket.setTicketEventDate(event.getEventDate().toString());

        ticketRepository.save(ticket);
        log.info("Ticket emitted with id {} for user '{}', eventId {}, category '{}'", ticket.getTicketId(), userName, event.getEventId(), category.getName());

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(event.getEventId());
        log.debug("Cache '{}' evicted for eventId id {} in method emmitTicket", CacheNames.REMAINING_TICKETS, event.getEventId());

        return Ticket.toTicketItemDto(ticket);
    }

    @Override
    @PreAuthorize(
        "hasRole('ADMIN') or " +
        "@ticketSecurity.isEventManager(#ticketId)"
    )
    public void deleteEmittedTicket(UUID ticketId) {
        log.info("Request to delete ticket with id {}", ticketId);

        var ticket = ticketRepository.findTicketWithEvent(ticketId).orElseThrow(() -> {
            log.warn("Ticket not found with id {}", ticketId);
            return new TicketNotFoundException();
        });

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(ticket.getEvent().getEventId());
        log.debug("Cache '{}' evicted for eventId {}", CacheNames.REMAINING_TICKETS, ticket.getEvent().getEventId());

        ticket.getTicketCategory().incrementTicketCategory();
        ticket.getEvent().incrementAvailableTickets();

        ticketRepository.deleteById(ticketId);
        log.info("Ticket with id {} deleted successfully", ticketId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TicketsDto listAllTickets(int page, int pageSize) {
        log.info("Listing all tickets: page {}, pageSize {}", page, pageSize);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.DESC, "ticketId");
        Page<TicketItemDto> ticketsPage = ticketRepository.findAllWithAssociations(pageRequest).map(Ticket::toTicketItemDto);

        log.debug("Found {} tickets on page {}", ticketsPage.getNumberOfElements(), page);
        return new TicketsDto(ticketsPage.getContent(), page, pageSize, ticketsPage.getTotalPages(), ticketsPage.getTotalElements());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public TicketsDto listAllUserTickets(int page, int pageSize) {
        String userName = jwtUtils.getAuthenticatedUsername();
        log.info("Listing tickets for user '{}' page {}, pageSize {}", userName, page, pageSize);

        User user = userService.findUserEntityByUserName(userName);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketId");
        var tickets = ticketRepository.findAllTicketsByUserId(user.getUserId(), pageRequest).map(Ticket::toTicketItemDto);

        log.debug("Found {} tickets for user '{}' on page {}", tickets.getNumberOfElements(), userName, page);
        return new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements());
    }

    @Cacheable(value = CacheNames.REMAINING_TICKETS, key = "#eventId")
    @Override
    @PreAuthorize("isAuthenticated()")
    public List<RemainingTicketCategoryDto> getAvailableTicketsByCategoryFromEvent(UUID eventId) {
        log.info("Getting available tickets by category for event {}", eventId);

        var event = eventService.findEventEntityById(eventId);

        var result = event.getTicketCategories().stream()
                .map(tc -> new RemainingTicketCategoryDto(tc.getName(), tc.getAvailableCategoryTickets()))
                .toList();

        log.debug("Available ticket categories for event {}: {}", eventId, result);
        return result;
    }

    @Override
    public TicketsDto getTicketsByCategoryId(Integer categoryId, int page, int pageSize) {
        log.info("Getting tickets for categoryId {}, page {}, pageSize {}", categoryId, page, pageSize);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticket_id");
        var tickets = ticketRepository.findTicketsByCategoryId(categoryId, pageRequest).map(Ticket::toTicketItemDto);

        log.debug("Found {} tickets for categoryId {} on page {}", tickets.getNumberOfElements(), categoryId, page);
        return new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements());
    }

    @Override
    public List<Ticket> findTicketsWithEventDetails(List<UUID> ticketIds) {
        log.info("Finding tickets with details for ticketIds {}", ticketIds);

        var tickets = ticketRepository.findAllByIdWithEvent(ticketIds);

        log.debug("Found {} tickets with event details", tickets.size());
        return tickets;
    }
}
