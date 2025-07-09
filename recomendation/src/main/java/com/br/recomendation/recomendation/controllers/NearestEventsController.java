package com.br.recomendation.recomendation.controllers;

import com.br.recomendation.recomendation.services.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Recommendations", description = "Endpoints for event recommendations.")
@RestController
@RequestMapping("/api/recommendations")
public class NearestEventsController {

    private final RecommendationService service;

    public NearestEventsController(RecommendationService service) {
        this.service = service;
    }

    @Operation(
            summary = "Find nearest events",
            description = "Returns a list of event IDs that are within a specified radius (in kilometers) of a given event.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of nearest event IDs."),
                    @ApiResponse(responseCode = "404", description = "The specified source event ID was not found.")
            }
    )
    @GetMapping("/nearest-events")
    public ResponseEntity<List<UUID>> getNearestEvents(
            @Parameter(
                    description = "The ID of the source event to calculate distance from.",
                    required = true,
                    example = "0df1a809-b6cf-49ee-9081-78adafefef27"
            )
            @RequestParam(name = "event_id") UUID eventId,

            @Parameter(
                    description = "The search radius in kilometers (km).",
                    required = true,
                    example = "50.0"
            )
            @RequestParam(name = "radius") Double radius
    ) {
        List<UUID> ids = service.getNearestEventIds(radius, eventId);
        return ResponseEntity.ok(ids);
    }
}