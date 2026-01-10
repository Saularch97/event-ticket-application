package com.example.booking.services.client.fallback;

import com.example.booking.dto.PaymentRequestDto;
import com.example.booking.exception.PaymentServiceUnavailableException;
import com.example.booking.services.client.PaymentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentFallback implements PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentFallback.class);

    @Override
    public String getCheckoutUrl(PaymentRequestDto dto) {
        log.error("CIRCUIT BREAKER: Fail to communicate order {} with payment processor", dto.orderId());

        throw new PaymentServiceUnavailableException("Payment service unavailable. Try again later");
    }
}