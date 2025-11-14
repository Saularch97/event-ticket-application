package com.example.booking.services;

import com.example.booking.dto.CityDataDto;
import com.example.booking.exception.CityDataNotFoundException;
import com.example.booking.services.client.GeoSearchClient;
import com.example.booking.services.intefaces.GeoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GeoServiceImpl implements GeoService {

    private static final Logger log = LoggerFactory.getLogger(GeoServiceImpl.class);

    private final GeoSearchClient client;

    public GeoServiceImpl(GeoSearchClient client) {
        this.client = client;
    }

    @Override
    public CityDataDto searchForCityData(String cityName) {
        log.info("Searching city data for '{}'", cityName);

        var response = client.search(cityName, "json", 1);

        if (response == null) {
            log.warn("City data not found for '{}'", cityName);
            throw new CityDataNotFoundException();
        }

        log.info("City data retrieved. City='{}', Latitude={}, Longitude={}",
                cityName, response.getFirst().lat(), response.getFirst().lon());

        return new CityDataDto(response.getFirst().lat(), response.getFirst().lon());
    }
}
