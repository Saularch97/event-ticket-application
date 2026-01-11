package com.example.booking.controller;

import com.example.booking.dto.TicketsDto;
import com.example.booking.controller.request.ticket.EmmitTicketRequest;
import com.example.booking.controller.response.ticket.AvailableTicketsResponse;
import com.example.booking.controller.response.ticket.CreateTicketResponse;
import com.example.booking.controller.response.ticket.TicketsResponse;
import com.example.booking.services.intefaces.QrCodeService;
import com.example.booking.services.intefaces.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Tickets", description = "Endpoints for creating and managing tickets.")
@RestController
@RequestMapping("/api/tickets") // Changed to /api/tickets for REST consistency
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;
    private final QrCodeService qrCodeService;

    public TicketController(TicketService ticketService, QrCodeService qrCodeService) {
        this.ticketService = ticketService;
        this.qrCodeService = qrCodeService;
    }

    @Operation(
            summary = "Emit a new ticket",
            description = "Creates a new ticket for a specific event and category. The userid is identified by the authentication token.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Ticket emitted successfully",
                            content =
                            @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = CreateTicketResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "Event or Ticket Category not found")
            }
    )
    @PostMapping
    public ResponseEntity<CreateTicketResponse> emmitTicket(
            @Valid @RequestBody EmmitTicketRequest request
    )  {
        var savedTicket = ticketService.emmitTicket(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedTicket.ticketId())
                .toUri();

        return ResponseEntity.created(location).body(
                new CreateTicketResponse(
                        savedTicket.ticketId(),
                        savedTicket.eventId(),
                        savedTicket.userId(),
                        savedTicket.ticketCategoryId()
                )
        );
    }

    @Operation(
            summary = "Delete an emitted ticket",
            description = "Deletes a ticket by its ID. Requires userid to be the ticket owner or an ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Ticket deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Ticket not found"),
                    @ApiResponse(responseCode = "403", description = "User is not authorized to delete this ticket")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable("id") UUID ticketId) {
        ticketService.deleteEmittedTicket(ticketId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List all tickets (Admin)",
            description = "Returns a paginated list of all tickets in the system. Requires ADMIN role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of tickets retrieved successfully",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TicketsResponse.class)))
            }
    )
    @GetMapping
    public ResponseEntity<TicketsResponse> listAllTickets(
            @Parameter(description = "Page number to retrieve") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of tickets per page") @RequestParam(defaultValue = "10") int pageSize
    ) {
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

    @Operation(
            summary = "List authenticated userid's tickets",
            description = "Retrieves a paginated list of all tickets belonging to the authenticated userid."
    )
    @GetMapping("/my-tickets")
    public ResponseEntity<TicketsResponse> listAllUserTickets(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize
    ) {
        var ticketsDto = ticketService.listAllUserTickets(page, pageSize);
        return ResponseEntity.ok(new TicketsResponse(ticketsDto.tickets(), ticketsDto.page(), ticketsDto.pageSize(), ticketsDto.totalPages(), ticketsDto.totalElements()));
    }

    @Operation(summary = "Get available tickets for an event", description = "Returns a list of ticket categories and the number of available tickets for a specific event.")
    @GetMapping("/available/{eventId}")
    public ResponseEntity<AvailableTicketsResponse> getAvailableTicketsForEvent(
            @Parameter(description = "ID of the event to check", required = true)
            @PathVariable UUID eventId
    ) {
        return ResponseEntity.ok(new AvailableTicketsResponse(ticketService.getAvailableTicketsByCategoryFromEvent(eventId)));
    }

    @Operation(summary = "List tickets by category ID", description = "Returns a paginated list of all tickets for a specific ticket category.")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<TicketsResponse> getTicketsByCategoryId(
            @Parameter(description = "ID of the ticket category", required = true) @PathVariable Integer categoryId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize
    ) {
        var ticketsDto = ticketService.getTicketsByCategoryId(categoryId, page, pageSize);
        return ResponseEntity.ok(new TicketsResponse(ticketsDto.tickets(), ticketsDto.page(), ticketsDto.pageSize(), ticketsDto.totalPages(), ticketsDto.totalElements()));
    }

    // TODO write tests
    @Operation(summary = "Generate QR Code for a ticket")
    @GetMapping(value = "/{ticketId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getTicketQrCode(@PathVariable UUID ticketId) {

        String qrContent = ticketId.toString();

        byte[] image = qrCodeService.generateQrCodeImage(qrContent, 250, 250);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    // TODO deixar aberto pra todos certo? No spring security
    @Operation(summary = "Verify ticket validity")
    @GetMapping("/verify/{ticketId}")
    public ResponseEntity<ValidationResponse> verifyTicket(@PathVariable UUID ticketId) {
        boolean isValid = ticketService.validateTicket(ticketId);

        return ResponseEntity.ok(new ValidationResponse(ticketId, isValid));
    }

    @Operation(summary = "Perform ticket check-in")
    @PostMapping("/checkin/{ticketId}")
    public ResponseEntity<Void> checkInTicket(@PathVariable UUID ticketId) {
        ticketService.performCheckIn(ticketId);
        return ResponseEntity.ok().build();
    }

    public record ValidationResponse(UUID ticketId, boolean valid) {}
}
