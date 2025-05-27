package com.br.recomendation.recomendation.services;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.br.recomendation.recomendation.consumer.EventRequestDto;
import com.br.recomendation.recomendation.model.EventData;
import com.br.recomendation.recomendation.repositories.RecomendationRepository;


@Service
public class RecomendationService {
    
    private final RecomendationRepository repository;

    public RecomendationService(RecomendationRepository repository) {
        this.repository = repository;
    }

    public List<UUID> getNearestEventIds(Double radius, UUID eventId) {
        
        EventData currentEvent = repository.findById(eventId).orElseThrow(() ->  new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found!"));
        
        var events = repository.findAll();

        List<UUID> nearestEventsIds = events.stream()
            .filter(event -> event.getLatitude() != null && event.getLongitude() != null && !event.getEventid().equals(currentEvent.getEventid()))
            .filter(event ->
                calcularDistanciaKm(
                    currentEvent.getLatitude(), currentEvent.getLongitude(),
                    event.getLatitude(), event.getLongitude()
                ) <= radius
            )
            .map(EventData::getEventid)
            .toList();

        return nearestEventsIds;
    }   


    public void saveEventData(EventRequestDto dto) {
        var eventData = new EventData(dto.eventId(), dto.latitude(), dto.longitude());
        repository.save(eventData);
    }


    public double calcularDistanciaKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Raio da Terra em km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

}
