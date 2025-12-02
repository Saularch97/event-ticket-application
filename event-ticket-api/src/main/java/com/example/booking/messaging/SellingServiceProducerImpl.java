package com.example.booking.messaging;

import com.example.booking.messaging.interfaces.SellingServiceProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

@Component
public class SellingServiceProducerImpl implements SellingServiceProducer {
    private static final Logger log = LoggerFactory.getLogger(SellingServiceProducerImpl.class);

    private static final String EXCHANGE_NAME = "event-request-exchange";
    private static final String ROUTING_KEY = "event-request-queue-key";

    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public SellingServiceProducerImpl(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }
}
