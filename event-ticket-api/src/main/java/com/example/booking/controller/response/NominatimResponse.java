package com.example.booking.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NominatimResponse(
         Double lat,
         Double lon
) {
}
