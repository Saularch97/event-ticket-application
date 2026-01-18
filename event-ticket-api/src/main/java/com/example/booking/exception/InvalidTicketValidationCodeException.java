package com.example.booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTicketValidationCodeException extends RuntimeException {

    public InvalidTicketValidationCodeException(String message) {
        super(message);
    }
}
