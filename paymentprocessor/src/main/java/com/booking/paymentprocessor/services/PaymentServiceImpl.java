package com.booking.paymentprocessor.services;

import com.booking.paymentprocessor.dto.PaymentRequestDto;
import com.booking.paymentprocessor.services.interfaces.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${app.checkout.success-url}")
    private String successUrl;

    @Value("${app.checkout.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe API Key initialized successfully.");
    }

    @Override
    @Retryable(
            retryFor = { ApiConnectionException.class },
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public Session createStripeSession(PaymentRequestDto dto) throws StripeException {
        log.info("Starting Stripe session creation for OrderId: {} with {} items.", dto.orderId(), dto.items().size());

        var lineItems = dto.items().stream()
                .map(item -> {
                    long amountInCents = 0L;
                    if (item.price() != null) {
                        amountInCents = item.price().movePointRight(2).longValue();
                    } else {
                        log.warn("Item with TicketId {} has NULL price! Defaulting to 0.00", item.ticketId());
                    }

                    return SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("BRL")
                                    .setUnitAmount(amountInCents)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Ticket")
                                            .setDescription("Ref Ticket: " + item.ticketId())
                                            .build())
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(dto.orderId().toString())
                .setCustomerEmail(dto.userEmail())
                .addAllLineItem(lineItems)
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey("checkout-" + dto.orderId().toString())
                .build();

        Session session = Session.create(params, options);

        log.info("Stripe session created successfully! SessionID: {}", session.getId());
        log.debug("Checkout URL generated: {}", session.getUrl());

        return session;
    }
}