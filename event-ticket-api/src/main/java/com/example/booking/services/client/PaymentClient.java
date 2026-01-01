package com.example.booking.services.client;

import com.example.booking.dto.PaymentRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "paymentprocessor", url = "${services.payment-processor.url}")
public interface PaymentClient {
    @PostMapping("/payments/checkout")
    String getCheckoutUrl(@RequestBody PaymentRequestDto dto);
}
