package com.example.booking.exception;

import com.example.booking.exception.base.ConflictException;

public class TicketAlreadyHaveAnOrderException extends ConflictException {
    public TicketAlreadyHaveAnOrderException(String message) {
        super(message);
    }
}
