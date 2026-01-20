package com.example.booking.exception;

import com.example.booking.exception.base.ConflictException;

public class OrderAlreadyConfirmedException extends ConflictException {
    public OrderAlreadyConfirmedException(String message) {
        super(message);
    }
}
