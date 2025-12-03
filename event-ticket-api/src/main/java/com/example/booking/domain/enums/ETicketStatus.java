package com.example.booking.domain.enums;

public enum ETicketStatus {
    PENDING,    // (1) Criado pelo OrderService. Estoque abatido temporariamente.
    PAID,       // (2) PaymentService confirmou sucesso.
    FAILED,     // (3) PaymentService recusou (sem saldo, cartão inválido).
    EXPIRED,    // (4) Cron Job do OrderService (Limpeza de pedidos travados).
    CANCELED,   // (5) Ação manual (Admin) ou estorno.
    REFUNDED    // (6) Pós-venda (Devolução do dinheiro).
}
