package com.example.booking.services;

import com.example.booking.controller.dto.OrderTicketDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.TicketsService;
import com.example.booking.util.JwtUtils;
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
    private final JwtUtils jwtUtils;

    public TicketsServiceImpl(TicketRepository ticketRepository, UserRepository userRepository, EventRepository eventRepository, JwtUtils jwtUtils) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.jwtUtils = jwtUtils;
    }

    public TicketItemDto orderTicket(
            OrderTicketDto dto,
            String token
    ) {

        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        Optional<User> user = userRepository.findByUserName(userName);
        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");

        Optional<Event> event = eventRepository.findById(dto.eventId());
        if (event.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found!");

        var ticket = new Ticket();
        ticket.setTicketOwner(user.get());
        ticket.setEvent(event.get());

        ticketRepository.save(ticket);

        return Ticket.toTicketItemDto(ticket);
    }

    public void deleteTicketOrder(UUID ticketId, String token) {
        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        var user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().name().equalsIgnoreCase(ERole.ROLE_ADMIN.name()));

        if (isAdmin || ticket.getTicketOwner().getUserName().equals(userName)) {
            ticketRepository.deleteById(ticketId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public TicketsDto listAllUserTickets(String token, int page, int pageSize) {
        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        Optional<User> user = userRepository.findByUserName(userName);

        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketId");

        var tickets = ticketRepository
                .findAllTicketsByUserId(user.get().getUserId(), pageRequest)
                .map(Ticket::toTicketItemDto);

        return new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements());
    }
}
