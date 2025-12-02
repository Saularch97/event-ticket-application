package com.example.booking.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record OrderItemDto(
        UUID orderId,
        java.math.BigDecimal orderPrice,
        List<TicketItemDto> tickets,
        UUID userid
) implements Serializable {
}
