package com.example.booking.domain.enums;

public enum ETicketStatus {
    RESERVED,   // (1) Assento bloqueado, aguardando início do pagamento/Pix.
    PENDING,    // (2) Pagamento enviado ao Gateway, aguardando Webhook (Async).
    PAID,       // (3) Sucesso! Webhook confirmou o pagamento.
    FAILED,     // (4) Cartão recusado ou erro no processamento.
    EXPIRED,    // (5) O tempo do Pix expirou ou o usuário desistiu (TTL).
    CANCELED,   // (6) Cancelado manualmente (pelo admin ou estorno).
    REFUNDED    // (7) Reembolsado (ótimo para mostrar complexidade extra).
}
