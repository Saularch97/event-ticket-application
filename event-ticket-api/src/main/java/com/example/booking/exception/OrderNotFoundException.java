package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException() {
        super("Order not found!");
    }
}
