package com.example.booking.controller.request.ticket;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateTicketCategoryRequest(

        @NotBlank(message = "Category name must not be blank")
        String name,

        @NotNull(message = "Price must not be null")
        @Positive(message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Available tickets must not be null")
        @Min(value = 1, message = "At least 1 ticket must be available")
        Integer availableCategoryTickets

) {}

