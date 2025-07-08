/*
package com.example.booking.services;

import com.example.booking.controller.request.order.PaymentInfoRequest;
import com.example.booking.domain.entities.Payment;
import com.example.booking.repository.PaymentRepository;
import com.example.booking.services.intefaces.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PaymentsServiceImpl implements PaymentService {

private final PaymentRepository paymentRepository;


public PaymentsServiceImpl(PaymentRepository paymentRepository, @Value("${STRIPE_SECRET_KEY}") String secretKey) {
    this.paymentRepository = paymentRepository;
    Stripe.apiKey = secretKey;
}


public PaymentIntent createPaymentIntent(PaymentInfoRequest paymentInfoRequest, JwtAuthenticationToken token) throws StripeException {

    List<String> paymentMethodTypes = new ArrayList<>();
    paymentMethodTypes.add("card");

    Map<String, Object> params = new HashMap<>();
    params.put("amount", paymentInfoRequest.amount());
    params.put("currency", paymentInfoRequest.currency());
    params.put("payment_method_types", paymentMethodTypes);


    var payment = new Payment();
    payment.setAmount(paymentInfoRequest.amount());
    payment.setUserEmail(token.getTokenAttributes().get("email").toString());

    paymentRepository.save(payment);

    return PaymentIntent.create(params);
}

public ResponseEntity<Payment> stripePayment(String userEmail) throws Exception {
    Payment payment = paymentRepository.findByUserEmail(userEmail);

    if (payment == null) {
        throw new Exception("Payment information is missing");
    }
    // Quando for confirmado zerar o que o consumidor deve
    payment.setAmount(0.0);
    var savedPayment = paymentRepository.save(payment);

    return new ResponseEntity<>(savedPayment,HttpStatus.OK);
}

}

 */
