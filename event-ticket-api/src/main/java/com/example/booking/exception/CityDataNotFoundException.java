package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;

public class CityDataNotFoundException extends NotFoundException {
    public CityDataNotFoundException() {
        super(HttpStatus.NOT_FOUND, "City data not found!");
    }
}
