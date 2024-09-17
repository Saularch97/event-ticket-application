package com.example.booking.entities.dto;

import java.util.List;
import java.util.UUID;

public record OrderItemDto(
        UUID orderId,
        Double orderPrice,
        List<TicketItemDto> tickets,
        UserDto user
) {
}
