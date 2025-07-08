package com.example.booking.services.intefaces;

import com.example.booking.controller.request.order.PaymentInfoRequest;
import com.example.booking.domain.entities.Payment;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public interface PaymentService {
    PaymentIntent createPaymentIntent(PaymentInfoRequest paymentInfoRequest, JwtAuthenticationToken token) throws StripeException;
    ResponseEntity<Payment> stripePayment(String userEmail) throws Exception;
}
