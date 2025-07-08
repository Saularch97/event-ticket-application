package com.example.booking.controller.response.order;

import com.example.booking.dto.OrderItemDto;

import java.io.Serializable;
import java.util.List;

public record OrdersResponse(
        List<OrderItemDto> orders,
        int page,
        int pageSize,
        int totalPages,
        long totalElements
) implements Serializable {
}
