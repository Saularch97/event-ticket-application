package com.example.booking.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record OrderItemDto(
        UUID orderId,
        Double orderPrice,
        List<TicketItemDto> tickets,
        UUID userid
) implements Serializable {
}
