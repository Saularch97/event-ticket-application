package com.example.booking.controller.request;

public record PaymentInfoRequest(Long amount, String currency, String receiptEmail) {
}
