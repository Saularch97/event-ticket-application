package com.example.booking.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotEmpty(message = "At least one ticket ID is required to create an order")
        @Size(min = 1, message = "At least one ticket ID must be provided")
        List<UUID> ticketIds
) {
}
