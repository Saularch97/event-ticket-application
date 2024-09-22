package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.CreateEventDto;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public interface EventsService {
    EventItemDto createEvent(CreateEventDto dto, String token);
    void deleteEvent(UUID eventId, JwtAuthenticationToken token);
    EventsDto listAllEvents(int page, int pageSize);
}
