package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;

public class TicketCategoryNotFoundException extends NotFoundException {
    public TicketCategoryNotFoundException(Long ticketCategoryId) {
        super("Ticket category not found for id: " + ticketCategoryId);
    }
}
