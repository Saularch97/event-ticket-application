package com.example.booking.exception;

import com.example.booking.exception.base.ConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EmailAlreadyExistsException extends ConflictException {
    public EmailAlreadyExistsException() {
        super("Email is already taken");
    }
}
