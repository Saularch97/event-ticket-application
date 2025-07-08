package com.example.booking.dto;

import java.io.Serializable;

public record RemainingTicketCategoryDto(
        String categoryName,
        Integer remainingTickets
) implements Serializable {
}
