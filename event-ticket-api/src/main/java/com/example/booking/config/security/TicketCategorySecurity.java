package com.example.booking.config.security;

import com.example.booking.repositories.TicketCategoryRepository;
import com.example.booking.util.JwtUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("ticketCategorySecurity")
public class TicketCategorySecurity {
    private final TicketCategoryRepository ticketCategoryRepository;
    private final JwtUtils jwtUtils;

    public TicketCategorySecurity(TicketCategoryRepository ticketCategoryRepository, JwtUtils jwtUtils) {
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.jwtUtils = jwtUtils;
    }

    public boolean isTicketCategoryOwner(Long ticketCategoryId) {
        UUID authenticatedUser = jwtUtils.getAuthenticatedUserId();

        return ticketCategoryRepository.findById(ticketCategoryId)
                .map(tc -> tc.getEvent().getEventOwner().getUserId().equals(authenticatedUser))
                .orElse(false);
    }
}
