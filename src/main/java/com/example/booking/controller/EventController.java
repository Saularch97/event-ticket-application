package com.example.booking.controller;

import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.response.EventResponse;
import com.example.booking.controller.response.EventsResponse;
import com.example.booking.services.EventsServiceImpl;
import com.example.booking.util.UriUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventsServiceImpl eventsServiceImpl;

    public EventController(EventsServiceImpl eventsServiceImpl) {
        this.eventsServiceImpl = eventsServiceImpl;
    }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(
            @RequestBody CreateEventRequest request,
            @RequestHeader(name = "Cookie", required = true) String token
    ) throws Exception {

        var eventItemDto = eventsServiceImpl.createEvent(request, token);

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
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") UUID eventId,
                                            @RequestHeader(name = "Cookie") String token) throws Exception {
         eventsServiceImpl.deleteEvent(eventId, token);

         return ResponseEntity.noContent().build();
    }

    @GetMapping("/events")
    public ResponseEntity<EventsResponse> listAllEvents(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var events = eventsServiceImpl.listAllEvents(page, pageSize);

        return ResponseEntity.ok(new EventsResponse(events.events(),
                events.page(),
                events.pageSize(),
                events.totalPages(),
                events.totalElements()));
    }

    @GetMapping("/userEvents")
    public ResponseEntity<EventsResponse> listAllEventsByUser(
            @RequestHeader(name = "Cookie") String token,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var events = eventsServiceImpl.listAllUserEvents(token, page, pageSize);

        return ResponseEntity.ok(new EventsResponse(events.events(), events.page(), events.pageSize(), events.totalPages(), events.totalElements()));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<EventItemDto>> listAllTrendingEvents() {
        var trendingEvents = eventsServiceImpl.getTopTrendingEvents();

        return ResponseEntity.ok(trendingEvents);
    }
}
