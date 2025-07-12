package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;

public class TicketNotFoundException extends NotFoundException {
    public TicketNotFoundException() {
        super("Ticket not found!");
    }
}
