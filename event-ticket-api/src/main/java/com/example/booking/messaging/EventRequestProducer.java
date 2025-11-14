package com.example.booking.messaging;

import com.example.booking.dto.RecommendEventDto;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface EventRequestProducer {
    void publishEventRecommendation(RecommendEventDto dto) throws JsonProcessingException;
}
