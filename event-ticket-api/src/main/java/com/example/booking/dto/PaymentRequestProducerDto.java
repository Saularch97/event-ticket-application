package com.example.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequestProducerDto(
        UUID orderId,
        BigDecimal amount,
        UUID userId,
        String payerEmail, // Útil para o Gateway enviar o recibo
        String description // Ex: "Pedido #1234 - 3 Ingressos"
) {
}
