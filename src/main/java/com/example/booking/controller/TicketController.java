package com.example.booking.controller;

import com.example.booking.entities.Ticket;
import com.example.booking.entities.dto.OrderTicketDto;
import com.example.booking.entities.dto.TicketItemDto;
import com.example.booking.entities.dto.TicketsDto;
import com.example.booking.repository.TicketRepository;
import com.example.booking.services.TicketsService;
import com.example.booking.util.UriUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketsService ticketsService;

    public TicketController(TicketRepository ticketRepository, TicketsService ticketsService) {
        this.ticketRepository = ticketRepository;
        this.ticketsService = ticketsService;
    }

    @PostMapping("/ticket")
    public ResponseEntity<TicketItemDto> orderTicket(
            @RequestBody OrderTicketDto dto,
            JwtAuthenticationToken token
    ) throws Exception {

        var savedTicket = ticketsService.orderTicket(dto, token);

        URI location = UriUtil.getUriLocation("ticketId", savedTicket.ticketId());

        return ResponseEntity.created(location).body(savedTicket);
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> deleteTicketOrder(@PathVariable("id") UUID ticketId,
                                                  JwtAuthenticationToken token) throws Exception {

        ticketsService.deleteTicketOrder(ticketId, token);

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
            JwtAuthenticationToken token,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var ticketsDto = ticketsService.listAllUserTickets(token, page, pageSize);

        return ResponseEntity.ok(ticketsDto);
    }
}
