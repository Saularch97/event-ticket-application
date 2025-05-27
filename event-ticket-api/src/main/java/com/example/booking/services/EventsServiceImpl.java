package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.dto.CityDataDto;
import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.enums.ERole;
import com.example.booking.repository.EventRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.GeoService;
import com.example.booking.util.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Transactional
@Service
public class EventsServiceImpl implements EventsService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final GeoService geoService;

    public EventsServiceImpl(EventRepository eventRepository, UserRepository userRepository, JwtUtils jwtUtils, GeoService geoService) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.geoService = geoService;
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
        event.setEventTicketPrice(request.eventPrice());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate date = LocalDate.parse(request.eventDate(), formatter);
        LocalDateTime dateTime = date.atTime(request.eventHour(), request.eventMinute());
        event.setEventDate(dateTime);
        event.setEventLocation(request.eventLocation());

        CityDataDto eventDataForRecommendationService = geoService.searchForCityData(event.getEventLocation());

        // TODO using the inputted name of the city, get lat and long
        // Send lat and long for an RabbitMq instance with the eventID
        // Content to study ;for implementation
        // Service Discovery:
        // https://www.youtube.com/results?search_query=rabbit+mq+with+spring
        // https://www.youtube.com/watch?v=weAruTI623k
        // https://www.youtube.com/watch?v=ZnECi2gatMs

        Integer availableTickets = 0;
        List<TicketCategory> ticketCategories = new ArrayList<>();
        for (var createTicketCategoryRequest : request.ticketCategories()) {
            var ticketCategory = new TicketCategory();
            ticketCategory.setEvent(event);
            ticketCategory.setName(createTicketCategoryRequest.name());
            ticketCategory.setPrice(createTicketCategoryRequest.price());
            ticketCategory.setAvailableCategoryTickets(createTicketCategoryRequest.availableCategoryTickets());
            ticketCategories.add(ticketCategory);
            availableTickets += createTicketCategoryRequest.availableCategoryTickets();
        }

        event.setAvailableTickets(availableTickets);
        event.setTicketCategories(ticketCategories);
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
        var user = userRepository.findByUserName(userName).orElseThrow(() -> new EntityNotFoundException("User not found!"));

        var events = eventRepository.findAllEventsByUserId(user.getUserId(),
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(Event::toEventItemDto);

        return new EventsDto(events.getContent(), page, pageSize, events.getTotalPages(), events.getTotalElements());
    }

    @Cacheable(
            value = CacheNames.TOP_EVENTS,
            key = "'topTrending'"
    )
    public List<EventItemDto> getTopTrendingEvents() {

        if(eventRepository.findAll().stream().filter(Event::getTrending).map(Event::toEventItemDto).toList().isEmpty()) {
            return new ArrayList<>();
        }

        return eventRepository.findAll().stream().filter(Event::getTrending).map(Event::toEventItemDto).toList();
    }

}
