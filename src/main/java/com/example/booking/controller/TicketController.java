package com.example.booking.controller;

import com.example.booking.controller.dto.*;
import com.example.booking.entities.Event;
import com.example.booking.entities.Role;
import com.example.booking.entities.Ticket;
import com.example.booking.entities.User;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@RestController
public class TicketController {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public TicketController(TicketRepository ticketRepository, UserRepository userRepository, EventRepository eventRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @PostMapping("/ticket")
    public ResponseEntity<Void> orderTicket(
            @RequestBody OrderTicketDto dto,
            JwtAuthenticationToken token
    ) throws Exception {

        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));
        if (user.isEmpty()) throw new Exception("User not exists!");

        Optional<Event> event = eventRepository.findById(dto.eventId());
        if (event.isEmpty()) throw new Exception("Event not exists!");

        var ticket = new Ticket();
        ticket.setTicketOwner(user.get());
        ticket.setEvent(event.get());
        // TODO sobre o preço mudar para o evento, pois a url de compra será "publica" já de evento não, isso pode deixar vulnerável para outros fazerem compras
        ticket.setTicketPrice(dto.ticketPrice());

        ticketRepository.save(ticket);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> deleteTicketOrder(@PathVariable("id") UUID ticketId,
                                                  JwtAuthenticationToken token) throws Exception {

        var user = userRepository.findById(UUID.fromString(token.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));


        // Transferir lógica para criar evento
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
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "ticketPrice")
        ).map(ticket ->
                new TicketItemDto(
                        ticket.getTicketId(),
                        new EventItemDto(
                                ticket.getEvent().getEventId(),
                                ticket.getEvent().getEventName(),
                                ticket.getEvent().getEventDate(),
                                ticket.getEvent().getEventDate().getHour(),
                                ticket.getEvent().getEventDate().getMinute()
                        ),
                        ticket.getTicketPrice(),
                        new UserDto(
                                ticket.getTicketOwner().getUserId(),
                                ticket.getTicketOwner().getUserName()
                        )
                )
        );

        return ResponseEntity.ok(new TicketsDto(
                tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements())
        );
    }

    @GetMapping("/userTickets")
    public ResponseEntity<TicketsDto> listAllUserTickets(
            JwtAuthenticationToken token,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));

        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "ticketPrice");

        var tickets = ticketRepository.findAllTicketsByUserId(UUID.fromString(token.getName()), pageRequest)
                .map(ticket ->
                    new TicketItemDto(
                        ticket.getTicketId(),
                        new EventItemDto(
                                ticket.getEvent().getEventId(),
                                ticket.getEvent().getEventName(),
                                ticket.getEvent().getEventDate(),
                                ticket.getEvent().getEventDate().getHour(),
                                ticket.getEvent().getEventDate().getMinute()
                        ),
                        ticket.getTicketPrice(),
                        new UserDto(
                                ticket.getTicketOwner().getUserId(),
                                ticket.getTicketOwner().getUserName()
                        )
                    ));

        return ResponseEntity.ok(new TicketsDto(tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements()));
    }
}
