package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;

public class TicketCategoryNotFoundException extends NotFoundException {
    public TicketCategoryNotFoundException(Long ticketCategoryId) {
        super(HttpStatus.NOT_FOUND, "Ticket category not found for id: " + ticketCategoryId);
    }
}
