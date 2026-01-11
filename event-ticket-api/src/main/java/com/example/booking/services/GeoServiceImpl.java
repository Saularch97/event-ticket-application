package com.example.booking.services;

import com.example.booking.controller.response.GeoSearchResponse;
import com.example.booking.dto.CityDataDto;
import com.example.booking.exception.CityDataNotFoundException;
import com.example.booking.services.client.GeoSearchClient;
import com.example.booking.services.intefaces.GeoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GeoServiceImpl implements GeoService {

    private static final Logger log = LoggerFactory.getLogger(GeoServiceImpl.class);

    private final GeoSearchClient client;

    public GeoServiceImpl(GeoSearchClient client) {
        this.client = client;
    }

    @Override
    public Optional<CityDataDto> searchForCityData(String cityName) {
        log.info("Searching city data for '{}'", cityName);

        List<GeoSearchResponse> response = client.search(cityName, "json", 1, "EventTicketApp-Portfolio/1.0");

        if (response == null || response.isEmpty()) {
            log.warn("City data not found for '{}'", cityName);
            return Optional.empty();
        }

        log.info("City data retrieved. City='{}', Latitude={}, Longitude={}",
                cityName, response.getFirst().lat(), response.getFirst().lon());

        return Optional.of(new CityDataDto(response.getFirst().lat(), response.getFirst().lon()));
    }
}
