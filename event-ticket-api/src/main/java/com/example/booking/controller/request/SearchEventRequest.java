package com.example.booking.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


public record SearchEventRequest(

        @NotBlank
        @Schema(description = "Name of event to search, it can be just an partial name too", example = "Show do Legado")
        String name,

        @NotBlank
        @Schema(description = "Event location", example = "Alfenas")
        String location,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Schema(description = "Date to start the search period (format ISO: YYYY-MM-DDTHH:MM:SS).", example = "2025-12-01T00:00:00")
        LocalDateTime startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Schema(description = "Date of the search period end (format ISO: YYYY-MM-DDTHH:MM:SS).", example = "2025-12-31T23:59:59")
        LocalDateTime endDate

) {}