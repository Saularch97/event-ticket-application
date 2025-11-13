package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;

public class EventNotFoundException extends NotFoundException {

    public EventNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Event not found");
    }
}
