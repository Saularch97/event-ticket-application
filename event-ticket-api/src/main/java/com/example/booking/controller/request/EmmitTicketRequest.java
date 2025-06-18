package com.example.booking.controller.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmmitTicketRequest(

        @NotNull(message = "Event ID is required!")
        UUID eventId,

        @NotNull(message = "Ticket category id is required!")
        Integer ticketCategoryId
) {}
