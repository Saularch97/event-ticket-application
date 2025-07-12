package com.example.booking.exception;

import com.example.booking.exception.base.ConflictException;

public class UserNameAlreadyExistsException extends ConflictException {

    public UserNameAlreadyExistsException() {
        super("User name already exists!");
    }
}
