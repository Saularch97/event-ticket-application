package com.example.booking.services;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
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

    public EventItemDto createEvent(CreateEventRequest request, String token) {
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
        event.setEventName(request.eventName());
        event.setEventPrice(request.eventPrice());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate date = LocalDate.parse(request.eventDate(), formatter);
        LocalDateTime dateTime = date.atTime(request.eventHour(), request.eventMinute());
        event.setEventDate(dateTime);
        event.setEventLocation(request.eventLocation());
        event.setAvailableTickets(request.availableTickets());

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

    public EventsDto listAllUserEvents(String token, int page, int pageSize) {

        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);
        Optional<User> user = userRepository.findByUserName(userName);
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "eventDate");

        var events = eventRepository.findAllEventsByUserId(user.get().getUserId(),
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(Event::toEventItemDto);

        return new EventsDto(events.getContent(), page, pageSize, events.getTotalPages(), events.getTotalElements());
    }
}
