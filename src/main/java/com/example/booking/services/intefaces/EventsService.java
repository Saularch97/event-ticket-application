package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.CreateEventDto;
import com.example.booking.controller.dto.EventItemDto;
import com.example.booking.controller.dto.EventsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public interface EventsService {
    EventItemDto createEvent(CreateEventDto dto, String token);
    ResponseEntity<?> deleteEvent(UUID eventId, String token);
    EventsDto listAllEvents(int page, int pageSize);
}
