package com.example.booking.exception;

import com.example.booking.exception.base.ConflictException;

public class TicketCategorySoldOutException extends ConflictException {
    public TicketCategorySoldOutException() {
        super("Category sold out!");
    }
}
