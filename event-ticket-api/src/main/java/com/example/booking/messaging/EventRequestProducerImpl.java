package com.example.booking.messaging;

import com.example.booking.messaging.interfaces.EventRequestProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import com.example.booking.dto.RecommendEventDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventRequestProducerImpl implements EventRequestProducer {
    private static final Logger log = LoggerFactory.getLogger(EventRequestProducerImpl.class);

    private static final String EXCHANGE_NAME = "event-request-exchange";
    private static final String ROUTING_KEY = "event-request-queue-key";

    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventRequestProducerImpl(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void publishEventRecommendation(RecommendEventDto dto) {
        String jsonPayload;
        // TODO análisar esses dois try catch e ver se são realmente necessários
        try {
            jsonPayload = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RecommendEventDto for RabbitMQ. DTO: {}. Error: {}", dto, e.getMessage(), e);
            return;
        }

        log.info("Publishing event recommendation. Exchange: {}, RoutingKey: {}, Payload: {}",
                EXCHANGE_NAME, ROUTING_KEY, jsonPayload);

        try {
            amqpTemplate.convertAndSend(
                    EXCHANGE_NAME,
                    ROUTING_KEY,
                    jsonPayload,
                    message -> {
                        message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return message;
                    }
            );

            log.info("Event recommendation published successfully. Payload: {}", jsonPayload);

        } catch (AmqpException e) {
            log.error("Failed to publish message to RabbitMQ. Exchange: {}, RoutingKey: {}, Payload: {}. Error: {}",
                    EXCHANGE_NAME, ROUTING_KEY, jsonPayload, e.getMessage(), e);
        }
    }
}