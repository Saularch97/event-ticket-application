package com.example.booking.messaging;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

import com.example.booking.controller.dto.RecomendEventDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventRequestProducerImpl implements EventRequestProducer{
    
    private final AmqpTemplate amqpTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventRequestProducerImpl(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }
    // O rabitt trabalha com esse flux
    // a mensagem e produzida e ela chega pro broker do rabbit
    // ela chega atraves deuma exchange
    // e por sua vez a exchange direciona a mensagem para as queues
    // e esse direcionamento e feito atraves dos bindings
    

    // O fluxo então é criar uma queue
    // depois cria uma uma exchange correspondete a queue
    // e o vinculo entre queue e exchange é feito pela routingkey
    public void publishEventRecommendation(RecomendEventDto dto) throws JsonProcessingException {
        amqpTemplate.convertAndSend(
            "event-request-exchange",
            "event-request-queue-key",
            objectMapper.writeValueAsString(dto)
        );
    }
}
