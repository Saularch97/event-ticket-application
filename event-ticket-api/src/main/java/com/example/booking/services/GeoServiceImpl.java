package com.example.booking.services;

import com.example.booking.dto.CityDataDto;
import com.example.booking.services.client.GeoSearchClient;
import com.example.booking.services.intefaces.GeoService;
import org.springframework.stereotype.Service;

@Service
public class GeoServiceImpl implements GeoService {

    private final GeoSearchClient client;

    public GeoServiceImpl(GeoSearchClient client) {
        this.client = client;
    }

    @Override
    public CityDataDto searchForCityData(String cityName) {
        var response = client.search(cityName, "json", 1).getFirst();
        return new CityDataDto(response.lat(), response.lon());
    }
}
