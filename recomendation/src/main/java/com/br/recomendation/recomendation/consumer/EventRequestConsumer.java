package com.br.recomendation.recomendation.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.br.recomendation.recomendation.services.RecommendationService;

@Component
public class EventRequestConsumer {

    private final RecommendationService service;

    @RabbitListener(queues = { "event-request-queue" })
    public void receiveMessage(EventRequestDto message) {
         try {
            // EventRequestDto dto = objectMapper.readValue(message, EventRequestDto.class);
            System.out.println(">>> Mensagem recebida na event-request-queue:");
            System.out.println(message + "\n");
            
            service.saveEventData(message);

        } catch (Exception e) {
            System.err.println("Erro ao desserializar a mensagem: " + e.getMessage());
        }
    }

    public EventRequestConsumer(RecommendationService service) {
        this.service = service;
    }
}
