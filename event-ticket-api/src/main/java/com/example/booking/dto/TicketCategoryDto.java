package com.example.booking.dto;

import java.math.BigDecimal;

public record TicketCategoryDto(String name, BigDecimal price, Integer availableTicketsFromCategory) {
}
