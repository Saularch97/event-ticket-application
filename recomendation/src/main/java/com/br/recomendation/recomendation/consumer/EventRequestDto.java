package com.br.recomendation.recomendation.consumer;

import java.util.UUID;

public record EventRequestDto(UUID eventId, Double latitude, Double longitude) {
    
}
