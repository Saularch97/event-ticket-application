package com.example.booking.controller.dto;

import java.util.UUID;

public record RecomendEventDto(UUID eventId, Double latitude, Double longitude) {
    
}
