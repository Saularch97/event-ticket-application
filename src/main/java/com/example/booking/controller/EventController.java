package com.example.booking.controller;

import com.example.booking.entities.dto.CreateEventDto;
import com.example.booking.entities.dto.EventItemDto;
import com.example.booking.entities.dto.EventsDto;
import com.example.booking.services.EventsService;
import com.example.booking.util.UriUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
public class EventController {

    private final EventsService eventsService;

    public EventController(EventsService eventsService) {
        this.eventsService = eventsService;
    }

    @PostMapping("/events")
    public ResponseEntity<EventItemDto> createEvent(
            @RequestBody CreateEventDto dto,
            JwtAuthenticationToken token
    ) throws Exception {

        var eventItemDto = eventsService.createEvent(dto, token);

        URI location = UriUtil.getUriLocation("eventId", eventItemDto.eventId());

        return ResponseEntity.created(location).body(eventItemDto);
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") UUID eventId,
                                             JwtAuthenticationToken token) throws Exception {

        eventsService.deleteEvent(eventId, token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/events")
    public ResponseEntity<EventsDto> listAllEvents(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        return ResponseEntity.ok(eventsService.listAllEvents(page, pageSize));
    }
}
