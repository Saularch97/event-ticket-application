package com.example.booking.messaging.producer;

import com.example.booking.dto.RecommendEventDto;
import com.example.booking.messaging.interfaces.EventRequestProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventRequestProducerImpl implements EventRequestProducer {

    private static final Logger log = LoggerFactory.getLogger(EventRequestProducerImpl.class);

    @Value("${rabbitmq.recommendation.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.recommendation.routing-key}")
    private String routingKey;

    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper;

    public EventRequestProducerImpl(AmqpTemplate amqpTemplate, ObjectMapper objectMapper) {
        this.amqpTemplate = amqpTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishEventRecommendation(RecommendEventDto dto) {
        String jsonPayload;

        try {
            jsonPayload = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Error: Failed to serialize RecommendEventDto. Data will be lost. DTO: {}. Error: {}", dto, e.getMessage(), e);
            return;
        }

        log.info("Publishing event recommendation. Exchange: {}, RoutingKey: {}, Payload: {}",
                exchangeName, routingKey, jsonPayload);

        try {
            amqpTemplate.convertAndSend(
                    exchangeName,
                    routingKey,
                    jsonPayload,
                    message -> {
                        message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        return message;
                    }
            );

            log.info("Event recommendation published successfully.");

        } catch (AmqpException e) {
            log.error("Failed to publish recommendation to RabbitMQ. Service might be down. Payload: {}. Error: {}",
                    jsonPayload, e.getMessage(), e);
        }
    }
}