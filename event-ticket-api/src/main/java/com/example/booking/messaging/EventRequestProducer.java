package com.example.booking.messaging;

import com.example.booking.dto.RecomendEventDto;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface EventRequestProducer {
    void publishEventRecommendation(RecomendEventDto dto) throws JsonProcessingException;
}
