package com.example.booking.services.intefaces;

import com.example.booking.controller.request.CreateEventRequest;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface EventsService {
    EventItemDto createEvent(CreateEventRequest dto, String token);
    void deleteEvent(UUID eventId, String token);
    EventsDto listAllEvents(int page, int pageSize);
    List<EventItemDto> getTopTrendingEvents();
}
