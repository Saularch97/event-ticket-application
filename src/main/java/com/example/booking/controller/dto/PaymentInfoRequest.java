package com.example.booking.controller.dto;

public record PaymentInfoRequest(Long amount, String currency, String receiptEmail) {
}
