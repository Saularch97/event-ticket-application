package com.example.booking.controller;

import com.example.booking.controller.dto.EventsDto;
import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.response.EventResponse;
import com.example.booking.controller.response.EventsResponse;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.util.UriUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventsService eventsService;

    public EventController(EventsService eventsService) {
        this.eventsService = eventsService;
    }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(
            @Valid
            @RequestBody CreateEventRequest request
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
                    eventItemDto.eventPrice(),
                    eventItemDto.availableTickets()
                )
        );
    }

    @DeleteMapping("/events/{id}")
    // TODO exemplo de permissionamento, incluir na API
//    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") UUID eventId) {
         eventsService.deleteEvent(eventId);

         return ResponseEntity.noContent().build();
    }

    @GetMapping("/events")
    public ResponseEntity<EventsResponse> listAllEvents(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var events = eventsService.listAllEvents(page, pageSize);

        return ResponseEntity.ok(
                new EventsResponse(
                    events.events(),
                    events.page(),
                    events.pageSize(),
                    events.totalPages(),
                    events.totalElements()
                )
        );
    }

    @GetMapping("/userEvents")
    public ResponseEntity<EventsResponse> listAllEventsByUser(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var events = eventsService.listAllUserEvents(page, pageSize);

        return ResponseEntity.ok(new EventsResponse(events.events(), events.page(), events.pageSize(), events.totalPages(), events.totalElements()));
    }

    @GetMapping("/trending")
    public ResponseEntity<?> listAllTrendingEvents() {
        var trendingEvents = eventsService.getTopTrendingEvents();

        return ResponseEntity.ok(trendingEvents);
    }

    @GetMapping("/availableUserEvents")
    public ResponseEntity<EventsResponse> listAvailableUserEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ) {
        var events = eventsService.listAllAvailableUserEvents(page, pageSize);

        return ResponseEntity.ok(new EventsResponse(events.events(), events.page(), events.pageSize(), events.totalPages(), events.totalElements()));
    }

    @Operation(
            summary = "Search new events with dynamic filters",
            description = "Returns a list of events based on optional search criteria such as name, location, and date range."
    )
    @GetMapping("/events/search")
    public ResponseEntity<EventsResponse> searchEvents(
            @Parameter(
                    description = "Complete of partial name for search",
                    example = "Show do Legado"
            )
            @RequestParam(required = false) String name,

            @Parameter(
                    description = "Event Location",
                    example = "Alfenas"
            )
            @RequestParam(required = false) String location,

            @Parameter(
                    description = "Start period from the search (format ISO: YYYY-MM-DDTHH:MM:SS).",
                    example = "2025-01-01T00:00:00"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @Parameter(
                    description = "End period from the search (format ISO: YYYY-MM-DDTHH:MM:SS).",
                    example = "2025-12-31T23:59:59"
            )
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @Parameter(
                    description = "page position",
                    example = "10"
            )
            @RequestParam(value = "page", defaultValue = "0") int page,

            @Parameter(
                    description = "page size",
                    example = "10"
            )
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize
    ) {
        EventsDto events = eventsService.searchEvents(name, location, startDate, endDate, page, pageSize);

        return ResponseEntity.ok(new EventsResponse(events.events(), page, pageSize, events.totalPages(), events.totalElements()));
    }
}
