package com.example.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class TicketNotPaidException extends RuntimeException {
    public TicketNotPaidException(String message) {
        super(message);
    }
}
