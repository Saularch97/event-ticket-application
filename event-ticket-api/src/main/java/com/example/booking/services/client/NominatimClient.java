package com.example.booking.services.client;

import com.example.booking.controller.response.NominatimResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "nominatimClient", url = "https://nominatim.openstreetmap.org")
public interface NominatimClient {
    @GetMapping(value = "/search", consumes = "application/json")
    List<NominatimResponse> search(
            @RequestParam("q") String query,
            @RequestParam("format") String format,
            @RequestParam("limit") int limit
    );
}
