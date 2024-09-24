package com.example.booking.services;

import com.example.booking.controller.dto.CreateEventDto;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    public EventItemDto createEvent(CreateEventDto dto, String token) {

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

    public ResponseEntity<?> deleteEvent(UUID eventId, String token) {

        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        boolean exists = eventRepository.existsById(eventId);  // Verifica se o item existe

        if (!exists) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);  // Retorna 404 se o item não foi encontrado
        }

        var user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.getRoles()
                .stream()
                .anyMatch(role -> role.getName().name().equalsIgnoreCase(ERole.ROLE_ADMIN.name()));

        if (isAdmin || event.getEventOwner().getUserName().equals(userName)) {
            eventRepository.deleteById(eventId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public EventsDto listAllEvents(int page, int pageSize) {
        var events = eventRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(Event::toEventItemDto);

        return new EventsDto(events.getContent(), page, pageSize, events.getTotalPages(), events.getTotalElements());
    }

//    public EventsDto listAllUserEvents(int page, int pageSize) {
//
//    }
}
