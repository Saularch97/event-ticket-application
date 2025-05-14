package com.example.booking.services.scheduler;

import com.example.booking.domain.entities.Event;
import com.example.booking.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventUpdater {

    private final EventRepository eventRepository;

    public EventUpdater(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // Exemplo: roda a cada hora
    // Verificando quantos ingressos estão sendo vendidos
    // ingresso quando é emitido tem um timestamp
    // Faz o calcula da ultima hora
    // subtrai 1h e ve quais ingressos foram vendidos
    // por exemplo, partindo das 12h ate as 12:30 venderam 600 ingressos
    // as 13 da tarde roda o job
    // ele subtrai 1h ou seja 13 - 1 = 12
    // Pego todos os ingressos que tem o timestamp depois das 12h

    @Scheduled(fixedRate = 3600000) // a cada 1h (em milissegundos)
    @Transactional
    public void updateEventStatuses() {
        List<Event> events = eventRepository.findAll();

        for (Event event : events) {

        }

        eventRepository.saveAll(events);
    }
}
