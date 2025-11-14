package com.br.recomendation.recomendation.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.br.recomendation.recomendation.services.RecommendationService;

@Component
public class EventRequestConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventRequestConsumer.class);

    private static final String EVENT_REQUEST_QUEUE = "event-request-queue";

    private final RecommendationService service;

    public EventRequestConsumer(RecommendationService service) {
        this.service = service;
    }

    @RabbitListener(queues = { EVENT_REQUEST_QUEUE })
    public void receiveMessage(EventRequestDto message) {
        try {
            log.info("Received message from queue [{}]. Payload: {}", EVENT_REQUEST_QUEUE, message);

            service.saveEventData(message);

            log.info("Successfully processed event request for payload: {}", message);

        } catch (Exception e) {
            log.error("Failed to process message from queue [{}]. Payload: {}. Error: {}",
                    EVENT_REQUEST_QUEUE, message, e.getMessage(), e);
        }
    }
}