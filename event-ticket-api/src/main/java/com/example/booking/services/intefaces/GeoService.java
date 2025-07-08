package com.example.booking.services.intefaces;

import com.example.booking.dto.CityDataDto;

public interface GeoService {
    CityDataDto searchForCityData(String cityName);
}
