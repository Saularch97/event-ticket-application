package com.example.booking.messaging.interfaces;

import com.example.booking.dto.PaymentRequestProducerDto;

public interface PaymentServiceProducer {
    void publishPayment(PaymentRequestProducerDto paymentRequestProducerDto);
}
