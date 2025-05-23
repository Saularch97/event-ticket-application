package com.example.booking.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmmitTicketRequest(

        @NotNull(message = "Event ID is required")
        UUID eventId,

        @NotBlank(message = "Ticket category name is required")
        String ticketCategoryName

) {}
