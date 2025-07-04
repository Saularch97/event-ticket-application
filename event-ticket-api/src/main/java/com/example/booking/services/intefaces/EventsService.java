package com.example.booking.services.intefaces;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import com.example.booking.domain.entities.Event;

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
}
