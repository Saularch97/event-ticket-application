package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.request.event.UpdateEventRequest;
import com.example.booking.dto.CityDataDto;
import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.dto.EventItemDto;
import com.example.booking.dto.EventsDto;
import com.example.booking.dto.RecommendEventDto;
import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.User;
import com.example.booking.dto.EventSummaryDto;
import com.example.booking.exception.EventNotFoundException;
import com.example.booking.messaging.EventRequestProducer;
import com.example.booking.repositories.EventRepository;
import com.example.booking.services.intefaces.EventsService;
import com.example.booking.services.intefaces.GeoService;
import com.example.booking.services.intefaces.TicketCategoryService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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

    public EventsServiceImpl(EventRepository eventRepository, UserService userService, JwtUtils jwtUtils, GeoService geoService, TicketCategoryService ticketCategoryService, EventRequestProducer producer) {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.geoService = geoService;
        this.ticketCategoryService = ticketCategoryService;
        this.producer = producer;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public EventItemDto createEvent(CreateEventRequest request) {
        UUID userId = jwtUtils.getAuthenticatedUserId();
        log.info("Creating event. UserId={}, EventName={}", userId, request.eventName());

        var user = userService.findUserEntityById(userId);

        var event = buildEventForRequest(request, user);

        CityDataDto cityData = geoService.searchForCityData(event.getEventLocation());

        Event savedEvent = eventRepository.save(event);
        ticketCategoryService.createTicketCategoriesForEvent(event, request.ticketCategories());
        publishEventRecommendation(savedEvent, cityData);

        log.info("Event created successfully. EventId={}, OwnerId={}", savedEvent.getEventId(), userId);
        return Event.toEventItemDto(savedEvent);
    }

    @PreAuthorize("hasRole('ADMIN') or @eventSecurity.isEventOwner(#eventId)")
    public void deleteEvent(UUID eventId) {
        log.info("Delete event requested. EventId={}", eventId);
        if (!eventRepository.existsById(eventId)) {
            log.warn("Delete failed: Event not found. EventId={}", eventId);
            throw new EventNotFoundException();
        }
        eventRepository.deleteById(eventId);
        log.info("Event deleted successfully. EventId={}", eventId);
    }

    @PreAuthorize("isAuthenticated()")
    public EventsDto listAllEvents(int page, int pageSize) {
        log.info("Listing all events. Page={}, PageSize={}", page, pageSize);
        var events = eventRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "eventDate")
        ).map(Event::toEventSummaryDto);

        return new EventsDto(events.getContent(), page, pageSize, events.getTotalPages(), events.getTotalElements());
    }

    @PreAuthorize("isAuthenticated()")
    public EventsDto listAllUserEvents(int page, int pageSize) {
        UUID userID = jwtUtils.getAuthenticatedUserId();
        log.info("Listing all events for user. UserId={}, Page={}, PageSize={}", userID, page, pageSize);

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
    @PreAuthorize("isAuthenticated()")
    public List<EventItemDto> getTopTrendingEvents() {
        log.info("Fetching top trending events");

        var trendingEvents = eventRepository.findAll().stream().filter(Event::getTrending).map(Event::toEventItemDto).toList();

        if(trendingEvents.isEmpty()) {
            log.info("No trending events found");
            return new ArrayList<>();
        }

        log.info("Found {} trending events", trendingEvents.size());
        return trendingEvents;
    }

    @Override
    public Event findEventEntityById(UUID eventId) {
        log.info("Fetching event entity by id. EventId={}", eventId);
        return eventRepository.findById(eventId).orElseThrow(() -> {
            log.warn("Event not found. EventId={}", eventId);
            return new EventNotFoundException();
        });
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public EventsDto listAllAvailableUserEvents(int page, int pageSize) {
        UUID userId = jwtUtils.getAuthenticatedUserId();
        log.info("Listing all available user events. UserId={}, Page={}, PageSize={}", userId, page, pageSize);

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
    @PreAuthorize("isAuthenticated()")
    public EventsDto searchEvents(String name, String location, LocalDateTime start, LocalDateTime end, int page, int pageSize) {
        log.info("Searching events with criteria. Name='{}', Location='{}', Start='{}', End='{}', Page={}, PageSize={}",
                name, location, start, end, page, pageSize);

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

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN') or @eventSecurity.isEventOwner(#eventId)")
    public void updateEvent(UUID eventId, UpdateEventRequest request) {
        log.info("Updating event. EventId={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Update failed: Event not found. EventId={}", eventId);
                    return new EntityNotFoundException("Event not found with ID: " + eventId);
                });

        event.setEventDate(request.eventDateTime());
        event.setEventLocation(request.eventLocation());
        event.setEventName(request.eventName());

        log.info("Event updated successfully. EventId={}", eventId);
    }

    private void publishEventRecommendation(Event savedEvent, CityDataDto cityData) {
        try {
            RecommendEventDto recommendEventDto = new RecommendEventDto(savedEvent.getEventId(), cityData.latitude(), cityData.longitude());
            producer.publishEventRecommendation(recommendEventDto);
            log.info("Event recommendation published for eventId={}", savedEvent.getEventId());
        } catch (JsonProcessingException e) {
            log.warn("Failed to send event recommendation to queue for eventId={}", savedEvent.getEventId(), e);
        }
    }

    private Event buildEventForRequest(CreateEventRequest request, User user) {
        var event = new Event();
        event.setEventOwner(user);
        event.setEventName(request.eventName());
        LocalDateTime dateTime = formatDate(request);
        event.setEventDate(dateTime);
        event.setEventLocation(request.eventLocation());
        return event;
    }

    private  LocalDateTime formatDate(CreateEventRequest request) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate date = LocalDate.parse(request.eventDate(), formatter);
        return date.atTime(request.eventHour(), request.eventMinute());
    }
}
