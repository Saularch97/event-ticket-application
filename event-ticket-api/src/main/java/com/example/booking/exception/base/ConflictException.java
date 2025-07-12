package com.example.booking.exception.base;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
