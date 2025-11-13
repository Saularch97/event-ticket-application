package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;

public class TicketNotFoundException extends NotFoundException {
    public TicketNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Ticket not found!");
    }
}
