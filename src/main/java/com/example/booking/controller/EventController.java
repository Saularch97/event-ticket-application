package com.example.booking.controller;

import com.example.booking.controller.dto.CreateEventDto;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import com.example.booking.services.EventsServiceImpl;
import com.example.booking.util.UriUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
public class EventController {

    private final EventsServiceImpl eventsServiceImpl;

    public EventController(EventsServiceImpl eventsServiceImpl) {
        this.eventsServiceImpl = eventsServiceImpl;
    }

    @PostMapping("/events")
    public ResponseEntity<EventItemDto> createEvent(
            @RequestBody CreateEventDto dto,
            @RequestHeader(name = "Cookie", required = true) String token
    ) throws Exception {

        var eventItemDto = eventsServiceImpl.createEvent(dto, token);

        URI location = UriUtil.getUriLocation("eventId", eventItemDto.eventId());

        return ResponseEntity.created(location).body(eventItemDto);
    }

    @DeleteMapping("/events/{id}")
    // TODO exemplo de permissionamento, incluir na API
//    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteEvent(@PathVariable("id") UUID eventId,
                                            @RequestHeader(name = "Cookie") String token) throws Exception {
        return eventsServiceImpl.deleteEvent(eventId, token);
    }

    @GetMapping("/events")
    public ResponseEntity<EventsDto> listAllEvents(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        return ResponseEntity.ok(eventsServiceImpl.listAllEvents(page, pageSize));
    }
}
