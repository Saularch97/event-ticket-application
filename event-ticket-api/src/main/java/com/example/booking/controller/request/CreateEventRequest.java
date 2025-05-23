package com.example.booking.controller.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateEventRequest(
        @NotBlank(message = "Event needs to have a name")
        String eventName,

        @NotBlank(message = "Event needs to have a date")
        String eventDate,

        @NotNull(message = "Event needs to have an hour")
        @Min(value = 0, message = "Hour must be between 0 and 23")
        @Max(value = 23, message = "Hour must be between 0 and 23")
        Integer eventHour,

        @NotNull(message = "Event needs to have a minute")
        @Min(value = 0, message = "Minute must be between 0 and 59")
        @Max(value = 59, message = "Minute must be between 0 and 59")
        Integer eventMinute,

        @NotBlank(message = "Event needs to have a location")
        String eventLocation,

        @NotNull(message = "Event needs to have a price")
        @Positive(message = "Price must be greater than zero")
        Double eventPrice,

        @NotNull(message = "Event needs to have at least one ticket category")
        @Size(min = 1, message = "At least one ticket category is required")
        @Valid
        List<CreateTicketCategoryRequest> ticketCategories
) {}

