package com.example.booking.services.client;

import com.example.booking.controller.response.GeoSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "nominatimClient", url = "https://nominatim.openstreetmap.org")
public interface GeoSearchClient {
    @GetMapping(value = "/search", consumes = "application/json")
    List<GeoSearchResponse> search(
            @RequestParam("q") String query,
            @RequestParam("format") String format,
            @RequestParam("limit") int limit
    );
}
