package com.example.booking.messaging;

import com.example.booking.dto.PaymentRequestProducerDto;
import com.example.booking.exception.MessageSerializationException;
import com.example.booking.messaging.interfaces.PaymentServiceProducer;
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
public class PaymentServiceProducerImpl implements PaymentServiceProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceProducerImpl.class);

    @Value("${rabbitmq.payment.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.payment.routing-key}")
    private String routingKey;

    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper;

    public PaymentServiceProducerImpl(AmqpTemplate amqpTemplate, ObjectMapper objectMapper) {
        this.amqpTemplate = amqpTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishPayment(PaymentRequestProducerDto dto) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentRequestProducerDto for RabbitMQ. DTO: {}. Error: {}", dto, e.getMessage(), e);
            throw new MessageSerializationException(e);
        }

        log.info("Publishing payment request. Exchange: {}, RoutingKey: {}, Payload: {}",
                exchangeName, routingKey, jsonPayload);

        try {
            amqpTemplate.convertAndSend(
                    exchangeName,
                    routingKey,
                    jsonPayload,
                    message -> {
                        message.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                        message.getMessageProperties().setCorrelationId(dto.orderId().toString());
                        return message;
                    }
            );

            log.info("Payment request published successfully. OrderId: {}", dto.orderId());

        } catch (AmqpException e) {
            log.error("Failed to publish payment message to RabbitMQ. Exchange: {}, RoutingKey: {}, Payload: {}. Error: {}",
                    exchangeName, routingKey, jsonPayload, e.getMessage(), e);
        }
    }
}