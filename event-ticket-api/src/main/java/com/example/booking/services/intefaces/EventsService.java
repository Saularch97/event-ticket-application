package com.example.booking.services.intefaces;

import com.example.booking.controller.request.event.CreateEventRequest;
import com.example.booking.controller.request.event.UpdateEventRequest;
import com.example.booking.dto.EventItemDto;
import com.example.booking.dto.EventsDto;
import com.example.booking.domain.entities.Event;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventsService {
    EventItemDto createEvent(CreateEventRequest dto);

    void deleteEvent(UUID eventId);

    EventsDto listAllEvents(int page, int pageSize);

    List<EventItemDto> getTopTrendingEvents();

    Event findEventEntityById(UUID eventId);

    EventsDto listAllUserEvents(int page, int pageSize);

    EventsDto listAllAvailableUserEvents(int page, int pageSize);

    EventsDto searchEvents(String name, String location, LocalDateTime start, LocalDateTime end, int page, int pageSize);

    void updateEvent(UUID id, UpdateEventRequest request);
}
