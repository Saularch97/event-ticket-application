package com.example.booking.controller.request.order;

public record PaymentInfoRequest(Long amount, String currency, String receiptEmail) {
}
