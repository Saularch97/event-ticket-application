package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.CityDataDto;

public interface GeoService {
    CityDataDto searchForCityData(String cityName);
}
