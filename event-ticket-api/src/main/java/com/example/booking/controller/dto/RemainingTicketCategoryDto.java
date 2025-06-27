package com.example.booking.controller.dto;

import java.io.Serializable;

public record RemainingTicketCategoryDto(
        String categoryName,
        Integer remainingTickets
) implements Serializable {
}
