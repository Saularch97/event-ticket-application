package com.example.booking.controller;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.response.EventResponse;
import com.example.booking.controller.response.EventsResponse;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.util.UriUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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

        return ResponseEntity.ok(new EventsResponse(events.events(),
                events.page(),
                events.pageSize(),
                events.totalPages(),
                events.totalElements()));
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
}
