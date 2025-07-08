package com.example.booking.messaging;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import com.example.booking.dto.RecomendEventDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventRequestProducerImpl implements EventRequestProducer{
    
    private final AmqpTemplate amqpTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventRequestProducerImpl(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }
   
    public void publishEventRecommendation(RecomendEventDto dto) throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(dto);

        amqpTemplate.convertAndSend(
            "event-request-exchange",
            "event-request-queue-key",
            json,
            message -> {
                message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            }
        );
    }
}
