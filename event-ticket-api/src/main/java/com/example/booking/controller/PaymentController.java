/*
package com.example.booking.controller;

import com.example.booking.controller.request.order.PaymentInfoRequest;
import com.example.booking.domain.entities.Payment;
import com.example.booking.services.intefaces.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/secure")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(@RequestBody PaymentInfoRequest paymentInfoRequest, JwtAuthenticationToken token)
            throws StripeException {

        try {
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(paymentInfoRequest, token);
            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (StripeException e) {
            // TODO melhorar logging da aplicação
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Verificar se o stripe manda confirmação do pagamento e inserir
    @PutMapping("/payment-complete")
    public ResponseEntity<Payment> stripePaymentComplete(JwtAuthenticationToken token)
            throws Exception {
        String userEmail = token.getTokenAttributes().get("email").toString();
        if (userEmail == null) {
            throw new Exception("User email is missing");
        }
        return paymentService.stripePayment(userEmail);
    }
}
*/