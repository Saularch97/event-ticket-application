package com.example.booking.entities.dto;

import java.util.List;

public record OrdersDto(
        List<OrderItemDto> orders,
        int page,
        int pageSize,
        int totalPages,
        long totalElements
) {
}
