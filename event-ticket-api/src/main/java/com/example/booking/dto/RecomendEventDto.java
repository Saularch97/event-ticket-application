package com.example.booking.dto;

import java.util.UUID;

public record RecomendEventDto(
        UUID eventId,
        Double latitude,
        Double longitude
) {
    
}
