package com.example.booking.controller.dto;

import java.util.List;

public record OrdersDto(
        List<OrderItemDto> orders,
        int page,
        int pageSize,
        int totalPages,
        long totalElements
) {
}
