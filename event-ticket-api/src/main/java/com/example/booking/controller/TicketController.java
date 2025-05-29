package com.example.booking.controller;

import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.controller.response.AvailableTicketsResponse;
import com.example.booking.controller.response.CreateTicketResponse;
import com.example.booking.controller.response.TicketsResponse;
import com.example.booking.services.TicketsServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketsServiceImpl ticketsServiceImpl;

    public TicketController(TicketsServiceImpl ticketsServiceImpl) {
        this.ticketsServiceImpl = ticketsServiceImpl;
    }

    @PostMapping("/ticket")
    public ResponseEntity<CreateTicketResponse> orderTicket(
            @Valid
            @RequestBody EmmitTicketRequest request
    )  {

        var savedTicket = ticketsServiceImpl.emmitTicket(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CreateTicketResponse(
                    savedTicket.ticketId(),
                    savedTicket.eventItem(),
                    savedTicket.userDto(),
                    savedTicket.ticketCategoryDto()
                )
        );
    }

    @DeleteMapping("/ticket/{id}")
    public ResponseEntity<Void> deleteTicketOrder(@PathVariable("id") UUID ticketId) {

        ticketsServiceImpl.deleteEmittedTicket(ticketId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tickets")
    public ResponseEntity<TicketsResponse> listAllTickets(@RequestParam(value = "page", defaultValue = "0") int page,
                                                          @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        TicketsDto ticketsDto = ticketsServiceImpl.listAllTickets(page, pageSize);
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

        var ticketsDto = ticketsServiceImpl.listAllUserTickets(page, pageSize);

        return ResponseEntity.ok(new TicketsResponse(ticketsDto.tickets(),
                ticketsDto.page(),
                ticketsDto.pageSize(),
                ticketsDto.totalPages(),
                ticketsDto.totalElements())
        );
    }

    @GetMapping("/availableTickets/{eventId}")
    public ResponseEntity<AvailableTicketsResponse> getAvailableTicketsForEvent(@PathVariable(name = "eventId") UUID eventId) {
        return ResponseEntity.ok(new AvailableTicketsResponse(ticketsServiceImpl.getAvailableTicketsByCategoryFromEvent(eventId)));
    }
}
