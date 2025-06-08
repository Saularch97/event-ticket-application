package com.example.booking.controller;

import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.controller.response.AvailableTicketsResponse;
import com.example.booking.controller.response.CreateTicketResponse;
import com.example.booking.controller.response.TicketsResponse;
import com.example.booking.services.TicketServiceImpl;
import com.example.booking.services.intefaces.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketServiceImpl ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/ticket")
    public ResponseEntity<CreateTicketResponse> orderTicket(
            @Valid
            @RequestBody EmmitTicketRequest request
    )  {

        var savedTicket = ticketService.emmitTicket(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedTicket.ticketId())
                .toUri();

        // Usa o método .created(), que já define o status 201 e o header Location
        return ResponseEntity.created(location).body(
                new CreateTicketResponse(
                        savedTicket.ticketId(),
                        savedTicket.eventId(),
                        savedTicket.userId(),
                        savedTicket.ticketCategoryId()
                )
        );
    }

    @DeleteMapping("/ticket/{id}")
    public ResponseEntity<Void> deleteTicketOrder(@PathVariable("id") UUID ticketId) {

        ticketService.deleteEmittedTicket(ticketId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tickets")
    public ResponseEntity<TicketsResponse> listAllTickets(@RequestParam(value = "page", defaultValue = "0") int page,
                                                          @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        TicketsDto ticketsDto = ticketService.listAllTickets(page, pageSize);
        return ResponseEntity.ok(
                new TicketsResponse(
                    ticketsDto.tickets(),
                    ticketsDto.page(),
                    ticketsDto.pageSize(),
                    ticketsDto.totalPages(),
                    ticketsDto.totalElements()
                )
        );
    }

    @GetMapping("/userTickets")
    public ResponseEntity<TicketsResponse> listAllUserTickets(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var ticketsDto = ticketService.listAllUserTickets(page, pageSize);

        return ResponseEntity.ok(new TicketsResponse(ticketsDto.tickets(),
                ticketsDto.page(),
                ticketsDto.pageSize(),
                ticketsDto.totalPages(),
                ticketsDto.totalElements())
        );
    }

    @GetMapping("/availableTickets/{eventId}")
    public ResponseEntity<AvailableTicketsResponse> getAvailableTicketsForEvent(@PathVariable(name = "eventId") UUID eventId) {
        return ResponseEntity.ok(new AvailableTicketsResponse(ticketService.getAvailableTicketsByCategoryFromEvent(eventId)));
    }
}
