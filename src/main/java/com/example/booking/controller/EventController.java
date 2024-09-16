package com.example.booking.controller;

import com.example.booking.controller.dto.*;
import com.example.booking.entities.Event;
import com.example.booking.entities.Role;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
public class EventController {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public EventController(EventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> createEvent(
            @RequestBody CreateEventDto dto,
            JwtAuthenticationToken token
    ) throws Exception {

        var user = userRepository.findById(UUID.fromString(token.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(Role.Values.ADMIN.name()));

        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var event = new Event();
        event.setEventOwner(user);
        event.setEventName(dto.eventName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate date = LocalDate.parse(dto.eventDate(), formatter);
        LocalDateTime dateTime = date.atTime(dto.eventHour(), dto.eventMinute());
        event.setEventDate(dateTime);
        event.setEventLocation(dto.eventLocation());

        eventRepository.save(event);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") UUID eventId,
                                             JwtAuthenticationToken token) throws Exception {

        var user = userRepository.findById(UUID.fromString(token.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(Role.Values.ADMIN.name()));

        if (isAdmin || event.getEventOwner().getUserId().equals(UUID.fromString(token.getName()))) {
            eventRepository.deleteById(eventId);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/events")
    public ResponseEntity<EventsDto> listAllEvents(@RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {


        // page does not work if you use id as properties
        var events = eventRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(event ->
                new EventItemDto(
                        event.getEventId(),
                        event.getEventName(),
                        event.getEventDate(),
                        event.getEventDate().getHour(),
                        event.getEventDate().getMinute()
                )
        );

        return ResponseEntity.ok(new EventsDto(
                events.getContent(), page, pageSize, events.getTotalPages(), events.getTotalElements())
        );
    }
}
