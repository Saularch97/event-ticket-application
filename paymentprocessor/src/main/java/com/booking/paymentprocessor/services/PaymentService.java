package com.booking.paymentprocessor.services;

import com.booking.paymentprocessor.dto.PaymentRequestDto;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${app.checkout.success-url}")
    private String successUrl;

    @Value("${app.checkout.cancel-url}")
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe API Key inicializada com sucesso.");
    }

    public Session createStripeSession(PaymentRequestDto dto) throws StripeException {
        // 2. Log de entrada
        log.info("Iniciando criação de sessão Stripe para OrderId: {} com {} itens.", dto.orderId(), dto.items().size());

        var lineItems = dto.items().stream()
                .map(item -> {
                    // 3. Proteção contra Preço Nulo (Defensive Programming)
                    long amountInCents = 0L;
                    if (item.price() != null) {
                        amountInCents = item.price().movePointRight(2).longValue();
                    } else {
                        log.warn("Item com TicketId {} veio com preço NULO! Usando 0.00", item.ticketId());
                    }

                    return SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("BRL")
                                    .setUnitAmount(amountInCents) // Usando a variável segura
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Ingresso") // Mudei para português, opcional
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

        log.info("Sessão Stripe criada com sucesso! SessionID: {}", session.getId());
        log.debug("URL de Checkout gerada: {}", session.getUrl());

        return session;
    }
}