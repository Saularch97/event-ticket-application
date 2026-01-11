package com.example.booking.exception;

import com.example.booking.exception.base.ConflictException;

public class TicketAlreadyUsedException extends ConflictException {
    public TicketAlreadyUsedException(String message) {
        super(message);
    }
}
