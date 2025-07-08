package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.dto.CityDataDto;
import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import com.example.booking.controller.dto.RecomendEventDto;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.dto.EventSummaryDto;
import com.example.booking.messaging.EventRequestProducer;
import com.example.booking.repositories.EventRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.GeoService;
import com.example.booking.services.intefaces.TicketCategoryService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
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
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final GeoService geoService;
    private final TicketCategoryService ticketCategoryService;
    private final EventRequestProducer producer;

    private static final Logger log = LoggerFactory.getLogger(EventsServiceImpl.class);


    public EventsServiceImpl(EventRepository eventRepository, UserService userRepository, JwtUtils jwtUtils, GeoService geoService, TicketCategoryService ticketCategoryService, EventRequestProducer producer) {
        this.eventRepository = eventRepository;
        this.userService = userRepository;
        this.jwtUtils = jwtUtils;
        this.geoService = geoService;
        this.ticketCategoryService = ticketCategoryService;
        this.producer = producer;
    }

    public EventItemDto createEvent(CreateEventRequest request) {

        UUID userId = jwtUtils.getAuthenticatedUserId();
        var user = userService.findUserEntityById(userId);

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

        CityDataDto cityData = geoService.searchForCityData(event.getEventLocation());

        Integer availableTickets = 0;
        List<TicketCategory> ticketCategories = ticketCategoryService.createTicketCategoriesForEvent(event, request.ticketCategories());

        for (var ticketCategory : ticketCategories) {
            availableTickets += ticketCategory.getAvailableCategoryTickets();
        }

        event.setAvailableTickets(availableTickets);
        event.setTicketCategories(ticketCategories);
        Event savedEvent = eventRepository.save(event);

        try {
            RecomendEventDto recomendEventDto = new RecomendEventDto(savedEvent.getEventId(), cityData.latitude(), cityData.longitude());
            producer.publishEventRecommendation(recomendEventDto);
        } catch (JsonProcessingException e) {
            log.warn("Failed to send event recommendation to queue", e);
        }

        return Event.toEventItemDto(savedEvent);
    }

    public void deleteEvent(UUID eventId) {
        UUID userId = jwtUtils.getAuthenticatedUserId();

        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }

        var user = userService.findUserEntityById(userId);

        var event = findEventEntityById(eventId);

        boolean isAdmin = User.userContainsAEspecificRole(user,ERole.ROLE_ADMIN.name());

        if (isAdmin || event.getEventOwner().getUserId().equals(userId)) {
            eventRepository.deleteById(eventId);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this event");
        }
    }

    public EventsDto listAllEvents(int page, int pageSize) {
        var events = eventRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(Event::toEventSummaryDto);

        return new EventsDto(events.getContent(), page, pageSize, events.getTotalPages(), events.getTotalElements());
    }

    public EventsDto listAllUserEvents(int page, int pageSize) {

        UUID userID = jwtUtils.getAuthenticatedUserId();
        var user = userService.findUserEntityById(userID);

        var events = eventRepository.findAllEventsByUserId(user.getUserId(),
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(Event::toEventSummaryDto);

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

    @Override
    public Event findEventEntityById(UUID eventId) {
        return eventRepository.findById(eventId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @Override
    public EventsDto listAllAvailableUserEvents(int page, int pageSize) {
        UUID userId = jwtUtils.getAuthenticatedUserId();

        var user = userService.findUserEntityById(userId);

        Page<EventSummaryDto> events = eventRepository.findAvailableEventsByOwner(user.getUserId(),
            PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        );

        return new EventsDto(
                events.getContent(),
                page,
                pageSize,
                events.getTotalPages(),
                events.getTotalElements()
        );
    }

    @Override
    public EventsDto searchEvents(String name, String location, LocalDateTime start, LocalDateTime end, int page, int pageSize) {

        Page<EventSummaryDto> eventsPage = eventRepository.findByCriteria(
                name,
                location,
                start,
                end,
                PageRequest.of(page, pageSize)
        );

        return new EventsDto(
                eventsPage.getContent(),
                page,
                pageSize,
                eventsPage.getTotalPages(),
                eventsPage.getTotalElements()
        );
    }
}
