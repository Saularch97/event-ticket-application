package com.example.booking.controller;

import com.example.booking.controller.dto.CreateTicketDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.entities.Role;
import com.example.booking.entities.Ticket;
import com.example.booking.entities.User;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TicketController {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketController(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/tickets")
    public ResponseEntity<Void> createTicket(
            @RequestBody CreateTicketDto dto,
            JwtAuthenticationToken token
    ) throws Exception {
        // we pass the token name as the id in the login controller
        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));
        if (user.isEmpty()) throw new Exception("User not exists!");

        var ticket = new Ticket();
        ticket.setTicketOwner(user.get());
        ticket.setTicketName(dto.ticketName());
        ticket.setTicketPrice(dto.ticketPrice());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate date = LocalDate.parse(dto.ticketDate(), formatter);
        LocalDateTime dateTime = date.atTime(dto.eventHour(), dto.eventMinute());
        ticket.setEventDate(dateTime);
        ticket.setEventLocation(dto.eventLocation());

        ticketRepository.save(ticket);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable("id") UUID ticketId,
                                             JwtAuthenticationToken token) throws Exception {

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/tickets")
    public ResponseEntity<TicketsDto> listAllTickets(@RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {


        // page does not work if you use id as properties
        var tickets = ticketRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(ticket ->
                new TicketItemDto(
                        ticket.getTicketId(),
                        ticket.getTicketName(),
                        ticket.getTicketPrice(),
                        ticket.getEventDate().toString(),
                        ticket.getEventDate().getHour(),
                        ticket.getEventDate().getMinute())
        );

        return ResponseEntity.ok(new TicketsDto(
                tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements())
        );
    }
}
