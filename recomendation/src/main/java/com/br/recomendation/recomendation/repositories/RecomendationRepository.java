package com.br.recomendation.recomendation.repositories;

import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.br.recomendation.recomendation.model.EventData;

public interface RecomendationRepository extends MongoRepository<EventData, UUID>{
    
}
