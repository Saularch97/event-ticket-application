package com.br.recomendation.recomendation.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EventRequestConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = { "event-request-queue" })
    public void receiveMessage(String message) {
         try {
            EventRequestDto dto = objectMapper.readValue(message, EventRequestDto.class);
            System.out.println(">>> Mensagem recebida na event-request-queue:");
            System.out.println(dto + "\n");
        } catch (Exception e) {
            System.err.println("Erro ao desserializar a mensagem: " + e.getMessage());
        }
    }
}
