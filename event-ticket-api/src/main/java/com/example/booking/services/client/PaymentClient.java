package com.example.booking.services.client;

import com.example.booking.dto.PaymentRequestDto;
import com.example.booking.services.client.fallback.PaymentFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-processor", url = "${services.payment-processor.url}", fallback = PaymentFallback.class)
public interface PaymentClient {
    @PostMapping("/payments/checkout")
    String getCheckoutUrl(@RequestBody PaymentRequestDto dto);
}
