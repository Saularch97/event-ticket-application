package com.example.booking.exception;

import com.example.booking.exception.base.ConflictException;

public class EventSoldOutException extends ConflictException {
    public EventSoldOutException() {
        super("Event sold out!");
    }
}
