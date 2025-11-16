package com.example.booking.exception;

public class InvalidRoleException extends IllegalArgumentException {
    public InvalidRoleException(String arg) {
        super("Invalid role for " + arg);
    }
}
