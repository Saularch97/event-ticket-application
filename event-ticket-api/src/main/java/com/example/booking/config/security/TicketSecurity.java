package com.example.booking.config.security;

import com.example.booking.repositories.TicketRepository;
import com.example.booking.util.JwtUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("ticketSecurity")
public class TicketSecurity {

    private final TicketRepository ticketRepository;
    private final JwtUtils jwtUtils;

    public TicketSecurity(TicketRepository ticketRepository, JwtUtils jwtUtils) {
        this.ticketRepository = ticketRepository;
        this.jwtUtils = jwtUtils;
    }

    public boolean isTicketOwner(UUID ticketId) {
        UUID authenticatedUser = jwtUtils.getAuthenticatedUserId();

        return ticketRepository.findById(ticketId)
                .map(ticket -> ticket.getTicketOwner().getUserId().equals(authenticatedUser))
                .orElse(false);
    }

    public boolean isEventManager(UUID ticketId) {
        UUID authenticatedUser = jwtUtils.getAuthenticatedUserId();

        return ticketRepository.findById(ticketId)
                .map(ticket -> ticket.getEvent().getEventOwner().getUserId().equals(authenticatedUser))
                .orElse(false);
    }
}
