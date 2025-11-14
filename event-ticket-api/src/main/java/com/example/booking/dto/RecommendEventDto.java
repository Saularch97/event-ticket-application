package com.example.booking.dto;

import java.util.UUID;

public record RecommendEventDto(
        UUID eventId,
        Double latitude,
        Double longitude
) {
    
}
