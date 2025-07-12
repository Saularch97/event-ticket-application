package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;

public class CityDataNotFoundException extends NotFoundException {
    public CityDataNotFoundException() {
        super("City data not found!");
    }
}
