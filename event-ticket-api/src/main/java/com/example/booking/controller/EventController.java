package com.example.booking.controller;

import com.example.booking.controller.request.event.UpdateEventRequest;
import com.example.booking.dto.EventsDto;
import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.response.event.EventResponse;
import com.example.booking.controller.response.event.EventsResponse;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.util.UriUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Events", description = "Endpoints for managing events.")
@RestController
@RequestMapping("/api/events")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventsService eventsService;

    public EventController(EventsService eventsService) {
        this.eventsService = eventsService;
    }

    @Operation(
            summary = "Create a new event",
            description = "Creates a new event and its ticket categories. Requires ADMIN role.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Event created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = EventResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "403", description = "Forbidden, userid is not an ADMIN")
            }
    )
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request
    ) throws Exception {
        var eventItemDto = eventsService.createEvent(request);
        URI location = UriUtil.getUriLocation("eventId", eventItemDto.eventId());
        return ResponseEntity.created(location).body(
                new EventResponse(
                        eventItemDto.eventId(),
                        eventItemDto.eventName(),
                        eventItemDto.eventDate(),
                        eventItemDto.eventHour(),
                        eventItemDto.eventMinute(),
                        eventItemDto.availableTickets()
                )
        );
    }

    @Operation(
            summary = "Delete an event",
            description = "Deletes an event by its ID. Requires userid to be the event owner or an ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Event deleted successfully"),
                    @ApiResponse(responseCode = "403", description = "Forbidden, userid is not allowed to delete this event"),
                    @ApiResponse(responseCode = "404", description = "Event not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "ID of the event to be deleted", required = true)
            @PathVariable("id") UUID eventId
    ) {
        eventsService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List all events",
            description = "Returns a paginated list of all events in the system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of events returned successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = EventsResponse.class)
                            )
                    )
            }
    )
    @GetMapping
    public ResponseEntity<EventsResponse> listAllEvents(
            @Parameter(description = "Page number to retrieve", example = "0") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Number of events per page", example = "10") @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ) {
        var events = eventsService.listAllEvents(page, pageSize);
        return ResponseEntity.ok(new EventsResponse(events.events(), events.page(), events.pageSize(), events.totalPages(), events.totalElements()));
    }


    @Operation(summary = "List all events created by the authenticated userid")
    @GetMapping("/my-events")
    public ResponseEntity<EventsResponse> listAllEventsByUser(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize
    ) {
        var events = eventsService.listAllUserEvents(page, pageSize);
        return ResponseEntity.ok(new EventsResponse(events.events(), events.page(), events.pageSize(), events.totalPages(), events.totalElements()));
    }

    @Operation(summary = "List top trending events")
    @GetMapping("/trending")
    public ResponseEntity<?> listAllTrendingEvents() {
        var trendingEvents = eventsService.getTopTrendingEvents();
        return ResponseEntity.ok(trendingEvents);
    }

    @Operation(summary = "List all available events not created by the authenticated userid")
    @GetMapping("/available")
    public ResponseEntity<EventsResponse> listAvailableUserEvents(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize
    ) {
        var events = eventsService.listAllAvailableUserEvents(page, pageSize);
        return ResponseEntity.ok(new EventsResponse(events.events(), events.page(), events.pageSize(), events.totalPages(), events.totalElements()));
    }

    @Operation(
            summary = "Search for events with dynamic filters",
            description = "Returns a paginated list of events based on optional search criteria."
    )
    @GetMapping("/search")
    public ResponseEntity<EventsResponse> searchEvents(
            @Parameter(description = "Partial or full event name") @RequestParam(required = false) String name,
            @Parameter(description = "Event location") @RequestParam(required = false) String location,
            @Parameter(description = "Start of date range (ISO format: YYYY-MM-DDTHH:MM:SS)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End of date range (ISO format: YYYY-MM-DDTHH:MM:SS)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int pageSize
    ) {
        EventsDto events = eventsService.searchEvents(name, location, startDate, endDate, page, pageSize);
        return ResponseEntity.ok(new EventsResponse(events.events(), page, pageSize, events.totalPages(), events.totalElements()));
    }

    @Operation(
            summary = "Update an existing event",
            description = "Updates the details of an existing event. Requires userid to be the event owner or an ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Event updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Event not found"),
                    @ApiResponse(responseCode = "403", description = "Forbidden")
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEvent(
            @Parameter(description = "ID of the event to be updated", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        eventsService.updateEvent(id, request);
        return ResponseEntity.noContent().build();
    }
}