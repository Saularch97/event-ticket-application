package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Order not found!");
    }
}
