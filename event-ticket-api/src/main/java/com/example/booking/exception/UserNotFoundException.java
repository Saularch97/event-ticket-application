package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException() {
        super("User not found!");
    }
}
