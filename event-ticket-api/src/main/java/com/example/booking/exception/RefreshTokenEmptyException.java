package com.example.booking.exception;

public class RefreshTokenEmptyException extends RuntimeException {
    public RefreshTokenEmptyException() {
        super("Refresh token is empty");
    }
}
