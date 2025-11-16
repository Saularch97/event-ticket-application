package com.example.booking.config.security;

import com.example.booking.repositories.EventRepository;
import com.example.booking.repositories.OrderRepository;
import com.example.booking.util.JwtUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("eventSecurity")
public class EventSecurity {
    private final EventRepository eventRepository;
    private final JwtUtils jwtUtils;

    public EventSecurity(EventRepository orderRepository, JwtUtils jwtUtils) {
        this.eventRepository = orderRepository;
        this.jwtUtils = jwtUtils;
    }

    public boolean isEventOwner(UUID eventId) {
        UUID authenticatedUser = jwtUtils.getAuthenticatedUserId();

        return eventRepository.findById(eventId)
                .map(event -> event.getEventOwner().getUserId().equals(authenticatedUser))
                .orElse(false);
    }
}
