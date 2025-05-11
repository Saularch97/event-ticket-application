package com.example.booking.services;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.enums.ERole;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class EventsServiceImpl implements EventsService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public EventsServiceImpl(EventRepository eventRepository, UserRepository userRepository, JwtUtils jwtUtils) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    public EventItemDto createEvent(CreateEventRequest dto, String token) {
        // TODO construir response no controller
        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        var user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().name().equalsIgnoreCase(ERole.ROLE_ADMIN.name()));

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        var event = new Event();
        event.setEventOwner(user);
        event.setEventName(dto.eventName());
        event.setEventPrice(dto.eventPrice());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate date = LocalDate.parse(dto.eventDate(), formatter);
        LocalDateTime dateTime = date.atTime(dto.eventHour(), dto.eventMinute());
        event.setEventDate(dateTime);
        event.setEventLocation(dto.eventLocation());

        Event savedEvent = eventRepository.save(event);

        return Event.toEventItemDto(savedEvent);
    }

    public void deleteEvent(UUID eventId, String token) {
        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }

        var user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().name().equalsIgnoreCase(ERole.ROLE_ADMIN.name()));

        if (isAdmin || event.getEventOwner().getUserName().equals(userName)) {
            eventRepository.deleteById(eventId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this event");
        }
    }



    public EventsDto listAllEvents(int page, int pageSize) {
        var events = eventRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(Event::toEventItemDto);

        return new EventsDto(events.getContent(), page, pageSize, events.getTotalPages(), events.getTotalElements());
    }
    // TODO implementar
//    public EventsDto listAllUserEvents(int page, int pageSize) {
//
//    }
}
