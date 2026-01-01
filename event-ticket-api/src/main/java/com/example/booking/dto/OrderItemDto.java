package com.example.booking.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemDto(
        UUID orderId,
        BigDecimal orderPrice,
        List<TicketItemDto> tickets,
        UUID userid,
        String checkoutUrl
) implements Serializable {
}
