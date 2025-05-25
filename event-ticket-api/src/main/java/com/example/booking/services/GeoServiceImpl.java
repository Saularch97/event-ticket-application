package com.example.booking.services;

import com.example.booking.controller.dto.CityDataDto;
import com.example.booking.services.client.NominatimClient;
import com.example.booking.services.intefaces.GeoService;
import org.springframework.stereotype.Service;

@Service
public class GeoServiceImpl implements GeoService {

    private final NominatimClient client;

    public GeoServiceImpl(NominatimClient client) {
        this.client = client;
    }

    @Override
    public CityDataDto searchForCityData(String cityName) {
        var response = client.search(cityName, "json", 1).getFirst();
        return new CityDataDto(response.lat(), response.lon());
    }
}
