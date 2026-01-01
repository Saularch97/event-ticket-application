package com.booking.paymentprocessor.controller;

import com.booking.paymentprocessor.dto.PaymentRequestDto;
import com.booking.paymentprocessor.services.PaymentService;
import com.stripe.exception.StripeException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public String createCheckoutSession(@RequestBody PaymentRequestDto request) throws StripeException {
        return paymentService.createStripeSession(request).getUrl();
    }
}