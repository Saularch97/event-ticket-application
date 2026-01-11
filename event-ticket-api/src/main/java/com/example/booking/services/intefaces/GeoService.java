package com.example.booking.services.intefaces;

import com.example.booking.dto.CityDataDto;

import java.util.Optional;

public interface GeoService {
    Optional<CityDataDto> searchForCityData(String cityName);
}
