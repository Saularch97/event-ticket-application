package com.example.booking.services;

import com.example.booking.controller.response.GeoSearchResponse;
import com.example.booking.dto.CityDataDto;
import com.example.booking.exception.CityDataNotFoundException;
import com.example.booking.services.client.GeoSearchClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;

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
    private static final double TEST_LAT = -21.4269;
    private static final double TEST_LON = -45.9467;

    @Test
    void searchForCityData_ShouldReturnDto_WhenCityIsFound() {
        CityDataDto mockResponse = new CityDataDto(TEST_LAT, TEST_LON);

        when(client.search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT))
                .thenReturn(List.of(new GeoSearchResponse(mockResponse.latitude(), mockResponse.longitude())));

        CityDataDto result = geoService.searchForCityData(TEST_CITY_NAME);

        assertNotNull(result);
        assertEquals(TEST_LAT, result.latitude());
        assertEquals(TEST_LON, result.longitude());
        verify(client).search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT);
    }

    @Test
    void searchForCityData_ShouldThrowCityDataNotFoundException_WhenClientReturnsNullInList() {
        when(client.search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT))
                .thenReturn(null);

        assertThrows(CityDataNotFoundException.class, () -> {
            geoService.searchForCityData(TEST_CITY_NAME);
        });

        verify(client).search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT);
    }

    @Test
    void searchForCityData_ShouldThrowException_WhenClientReturnsEmptyList() {
        when(client.search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT))
                .thenReturn(List.of());

        assertThrows(NoSuchElementException.class, () -> {
            geoService.searchForCityData(TEST_CITY_NAME);
        });

        verify(client).search(TEST_CITY_NAME, PARAM_FORMAT, PARAM_LIMIT);
    }
}