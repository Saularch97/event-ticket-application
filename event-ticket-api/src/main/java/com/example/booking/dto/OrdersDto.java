package com.example.booking.dto;

import java.io.Serializable;
import java.util.List;

public record OrdersDto(
        List<OrderItemDto> orders,
        int page,
        int pageSize,
        int totalPages,
        long totalElements
) implements Serializable {
}
