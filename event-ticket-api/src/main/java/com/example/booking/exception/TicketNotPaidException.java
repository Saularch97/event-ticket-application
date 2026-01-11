package com.example.booking.exception;

import jakarta.ws.rs.BadRequestException;

public class TicketNotPaidException extends BadRequestException {
    public TicketNotPaidException(String message) {
        super(message);
    }
}
