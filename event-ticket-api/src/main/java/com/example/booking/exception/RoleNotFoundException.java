package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;

public class RoleNotFoundException extends NotFoundException {
    public RoleNotFoundException() {
        super("Role not found!");
    }
}
