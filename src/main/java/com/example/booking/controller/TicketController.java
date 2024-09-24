package com.example.booking.controller;

import com.example.booking.domain.entities.Ticket;
import com.example.booking.controller.dto.OrderTicketDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.repository.TicketRepository;
import com.example.booking.services.TicketsServiceImpl;
import com.example.booking.util.UriUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketsServiceImpl ticketsServiceImpl;

    public TicketController(TicketRepository ticketRepository, TicketsServiceImpl ticketsServiceImpl) {
        this.ticketRepository = ticketRepository;
        this.ticketsServiceImpl = ticketsServiceImpl;
    }

    @PostMapping("/ticket")
    public ResponseEntity<TicketItemDto> orderTicket(
            @RequestBody OrderTicketDto dto,
            @RequestHeader(name = "Cookie") String token
    ) throws Exception {

        var savedTicket = ticketsServiceImpl.orderTicket(dto, token);

        return new ResponseEntity<>(savedTicket, HttpStatus.CREATED);
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> deleteTicketOrder(@PathVariable("id") UUID ticketId,
                                                  @RequestHeader(name = "Cookie") String token) throws Exception {

        ticketsServiceImpl.deleteTicketOrder(ticketId, token);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/tickets")
    public ResponseEntity<TicketsDto> listAllTickets(@RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {


        // page does not work if you use id as properties
        var tickets = ticketRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "ticketId")
        ).map(Ticket::toTicketItemDto);

        return ResponseEntity.ok(new TicketsDto(
                tickets.getContent(), page, pageSize, tickets.getTotalPages(), tickets.getTotalElements())
        );
    }

    @GetMapping("/userTickets")
    public ResponseEntity<TicketsDto> listAllUserTickets(
            @RequestHeader(name = "Cookie") String token,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var ticketsDto = ticketsServiceImpl.listAllUserTickets(token, page, pageSize);

        return ResponseEntity.ok(ticketsDto);
    }
}
