package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.domain.enums.ETicketStatus;
import com.example.booking.dto.RemainingTicketCategoryDto;
import com.example.booking.dto.TicketItemDto;
import com.example.booking.dto.TicketsDto;
import com.example.booking.controller.request.ticket.EmmitTicketRequest;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.exception.*;
import com.example.booking.repositories.TicketRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.TicketCategoryService;
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

import java.time.LocalDateTime;
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
    private final TicketCategoryService ticketCategoryService;

    public TicketServiceImpl(TicketRepository ticketRepository, UserService userService, EventsService eventService, JwtUtils jwtUtils, CacheManager cacheManager, TicketCategoryService ticketCategoryService) {
        this.ticketRepository = ticketRepository;
        this.userService = userService;
        this.eventService = eventService;
        this.jwtUtils = jwtUtils;
        this.cacheManager = cacheManager;
        this.ticketCategoryService = ticketCategoryService;
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public TicketItemDto emmitTicket(EmmitTicketRequest request) {
        String userName = jwtUtils.getAuthenticatedUsername();
        log.info("User '{}' requested to emit ticket for eventId={}, ticketCategoryId={}", userName, request.eventId(), request.ticketCategoryId());

        User user = userService.findUserEntityByUserName(userName);
        Event event = eventService.findEventEntityById(request.eventId());

        var ticketCategory = ticketCategoryService.reserveOneTicket(request.ticketCategoryId());

        eventService.decrementAvailableTickets(event.getEventId());

        Ticket ticket = buildTicket(user, event, ticketCategory);
        ticketRepository.save(ticket);

        log.info("Ticket emitted with id {} for user '{}', eventId {}, category '{}'", ticket.getTicketId(), userName, event.getEventId(), ticketCategory.getName());

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(event.getEventId());
        log.debug("Cache '{}' evicted for eventId id {} in method emmitTicket", CacheNames.REMAINING_TICKETS, event.getEventId());

        return Ticket.toTicketItemDto(ticket);
    }

    @Override
    @PreAuthorize(
        "hasRole('ADMIN') or " +
        "@ticketSecurity.isEventManager(#ticketId) or "  +
        "@ticketSecurity.isTicketOwner(#ticketId)"
    )
    public void deleteEmittedTicket(UUID ticketId) {
        log.info("Request to delete ticket with id {}", ticketId);

        var ticket = ticketRepository.findTicketWithEvent(ticketId).orElseThrow(() -> {
            log.warn("Ticket not found with id {}", ticketId);
            return new TicketNotFoundException();
        });

        Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(ticket.getEvent().getEventId());
        log.debug("Cache '{}' evicted for eventId {}", CacheNames.REMAINING_TICKETS, ticket.getEvent().getEventId());

        ticketCategoryService.incrementTicketCategory(ticket.getTicketCategory().getTicketCategoryId());
        eventService.incrementAvailableTickets(ticket.getEvent().getEventId());

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

    @Override
    public List<Ticket> findAndValidateAvailableTickets(List<UUID> ticketIds) {
        List<Ticket> tickets = ticketRepository.findTicketsWithEventDetails(ticketIds);

        if (tickets.size() != ticketIds.size()) {
            log.error("Tickets size mismatch!");
            throw new TicketNotFoundException();
        }

        boolean hasUnavailableTicket = tickets.stream().anyMatch(t -> t.getOrder() != null);

        if (hasUnavailableTicket) {
            throw new TicketAlreadyHaveAnOrderException("One or more tickets are not available.");
        }

        return tickets;
    }

    @Override
    public Boolean validateTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .map(ticket -> {
                    return ticket.getTicketStatus() == ETicketStatus.PAID;
                })
                .orElse(false);
    }


    @Transactional
    public void performCheckIn(UUID ticketId, String validationCode) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(TicketNotFoundException::new);

        if (ticket.getValidationCode() == null || !ticket.getValidationCode().equalsIgnoreCase(validationCode)) {
            throw new InvalidTicketValidationCodeException("Invalid validation code for ticket " + ticket.getTicketId());
        }

        if (ticket.getTicketStatus() == ETicketStatus.USED) {
            throw new TicketAlreadyUsedException("Ticket already used!");
        }

        if (ticket.getTicketStatus() != ETicketStatus.PAID) {
            throw new TicketNotPaidException("Ticket is not paid. Status: " + ticket.getTicketStatus());
        }

        ticket.setTicketStatus(ETicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());

        ticketRepository.save(ticket);
        log.info("Check-in successful for TicketID: {}", ticketId);
    }

    public String generateNewValidationCode(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(TicketNotFoundException::new);

        if (ticket.getTicketStatus() == ETicketStatus.USED) {
            throw new TicketAlreadyUsedException("Ticket already used. Cannot generate new QR Code.");
        }

        String newToken = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        ticket.setValidationCode(newToken);
        ticketRepository.save(ticket);

        log.info("New validation token generated for Ticket {}: {}", ticketId, newToken);

        return newToken;
    }

    private static Ticket buildTicket(User user, Event event, TicketCategory ticketCategory) {
        Ticket ticket = new Ticket();
        ticket.setTicketOwner(user);
        ticket.setEvent(event);
        ticket.setTicketCategory(ticketCategory);
        ticket.setTicketCategoryName(ticketCategory.getName());
        ticket.setTicketPrice(ticketCategory.getPrice());
        ticket.setTicketEventLocation(event.getEventLocation());
        ticket.setTicketEventDate(event.getEventDate().toString());
        ticket.setTicketStatus(ETicketStatus.PENDING);
        return ticket;
    }
}
