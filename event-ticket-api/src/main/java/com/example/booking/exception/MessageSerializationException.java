package com.example.booking.exception;

public class MessageSerializationException extends RuntimeException {
    public MessageSerializationException(Throwable cause) {
        super("Error processing payment data", cause);
    }
}
