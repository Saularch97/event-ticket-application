package com.example.booking.services;

import com.example.booking.controller.response.GeoSearchResponse;
import com.example.booking.dto.CityDataDto;
import com.example.booking.services.client.GeoSearchClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoServiceTest {

    @Mock
    private GeoSearchClient client;

    @InjectMocks
    private GeoServiceImpl geoService;

    private static final String TEST_CITY_NAME = "Alfenas";
    private static final String PARAM_FORMAT = "json";
    private static final int PARAM_LIMIT = 1;
    private static final String USER_AGENT = "EventTicketApp-Portfolio/1.0";
    private static final double TEST_LAT = -21.4269;
    private static final double TEST_LON = -45.9467;

    @Test
    void searchForCityData_ShouldReturnDto_WhenCityIsFound() {
        CityDataDto mockData = new CityDataDto(TEST_LAT, TEST_LON);

        when(client.search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT, USER_AGENT))
                .thenReturn(List.of(new GeoSearchResponse(mockData.latitude(), mockData.longitude())));

        Optional<CityDataDto> result = geoService.searchForCityData(TEST_CITY_NAME);

        assertTrue(result.isPresent());
        assertEquals(TEST_LAT, result.get().latitude());
        assertEquals(TEST_LON, result.get().longitude());

        verify(client).search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT, USER_AGENT);
    }

    @Test
    void searchForCityData_ShouldReturnEmpty_WhenClientReturnsNull() {
        when(client.search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT, USER_AGENT))
                .thenReturn(null);

        Optional<CityDataDto> result = geoService.searchForCityData(TEST_CITY_NAME);

        assertTrue(result.isEmpty());

        verify(client).search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT, USER_AGENT);
    }

    @Test
    void searchForCityData_ShouldReturnEmpty_WhenClientReturnsEmptyList() {
        when(client.search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT, USER_AGENT))
                .thenReturn(Collections.emptyList());

        Optional<CityDataDto> result = geoService.searchForCityData(TEST_CITY_NAME);

        assertTrue(result.isEmpty());

        verify(client).search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT, USER_AGENT);
    }
}