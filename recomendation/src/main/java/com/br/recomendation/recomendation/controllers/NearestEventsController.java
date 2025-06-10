package com.br.recomendation.recomendation.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.br.recomendation.recomendation.services.RecommendationService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NearestEventsController {
    private final RecommendationService service;

    public NearestEventsController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping("/nearestEvents")
    public ResponseEntity<List<UUID>> getNearestEvents(@RequestParam(name = "event_id") UUID eventId, @RequestParam(name = "radius") Double radius) {
        List<UUID> ids = service.getNearestEventIds(radius, eventId);

        return ResponseEntity.ok(ids);
    }
}
