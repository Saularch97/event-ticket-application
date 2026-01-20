package com.example.booking.exception;

import com.example.booking.exception.base.NotFoundException;
import org.springframework.http.HttpStatus;

public class CityDataNotFoundException extends NotFoundException {
    public CityDataNotFoundException() {
        super("City data not found!");
    }
}
