package com.example.booking.services;

import com.example.booking.controller.dto.OrderTicketDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.User;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.TicketsService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Transactional
@Service
public class TicketsServiceImpl implements TicketsService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public TicketsServiceImpl(TicketRepository ticketRepository, UserRepository userRepository, EventRepository eventRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    public TicketItemDto orderTicket(
            OrderTicketDto dto,
            JwtAuthenticationToken token
    ) {
        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));
        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");

        Optional<Event> event = eventRepository.findById(dto.eventId());
        if (event.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found!");

        var ticket = new Ticket();
        ticket.setTicketOwner(user.get());
        ticket.setEvent(event.get());

        return ticketRepository.save(ticket).toTicketItemDto();
    }

    public void deleteTicketOrder(UUID ticketId, JwtAuthenticationToken token) {
        var user = userRepository.findById(UUID.fromString(token.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(Role.Values.ADMIN.name()));

        if (isAdmin || ticket.getTicketOwner().getUserId().equals(UUID.fromString(token.getName()))) {
            ticketRepository.deleteById(ticketId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public TicketsDto listAllUserTickets(JwtAuthenticationToken token, int page, int pageSize) {
        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));

        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketId");

        var tickets = ticketRepository
                .findAllTicketsByUserId(UUID.fromString(token.getName()), pageRequest)
                .map(Ticket::toTicketItemDto);

        return new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements());
    }
}
