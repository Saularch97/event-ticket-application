package com.example.booking.services.client.fallback;

import com.example.booking.controller.response.GeoSearchResponse;
import com.example.booking.services.client.GeoSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Component
public class GeoSearchClientFallback implements GeoSearchClient {

    private static final Logger log = LoggerFactory.getLogger(GeoSearchClientFallback.class);

    @Override
    public List<GeoSearchResponse> search(
            @RequestParam("q") String query,
            @RequestParam("format") String format,
            @RequestParam("limit") int limit,
            @RequestHeader("User-Agent") String userAgent
    ) {
        log.warn("Was not possible to get an location for '{}'. The service might be unavailable.", query);

        return Collections.emptyList();
    }
}
