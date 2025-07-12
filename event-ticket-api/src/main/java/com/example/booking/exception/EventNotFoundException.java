package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;

public class EventNotFoundException extends NotFoundException {

    public EventNotFoundException() {
        super("Event not found");
    }
}
