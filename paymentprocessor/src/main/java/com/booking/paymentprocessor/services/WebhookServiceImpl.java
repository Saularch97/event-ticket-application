package com.booking.paymentprocessor.services;

import com.booking.paymentprocessor.services.interfaces.WebhookService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookServiceImpl implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);
    private final RabbitTemplate rabbitTemplate;
    private final String endpointSecret;

    public WebhookServiceImpl(RabbitTemplate rabbitTemplate,
                              @Value("${stripe.webhook.secret}") String endpointSecret) {
        this.rabbitTemplate = rabbitTemplate;
        this.endpointSecret = endpointSecret;
    }

    @Override
    public void handleWebhook(String payload, String sigHeader) throws SignatureVerificationException, EventDataObjectDeserializationException {

        Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

        if ("checkout.session.completed".equals(event.getType())) {
            processCheckoutSession(event);
        } else {
            log.debug("Event ignored: {}", event.getType());
        }
    }

    private void processCheckoutSession(Event event) throws EventDataObjectDeserializationException {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;

        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            log.warn("Event API version differs. Attempting unsafe deserialization...");
            stripeObject = dataObjectDeserializer.deserializeUnsafe();
        }

        if (stripeObject instanceof Session session) {
            String orderId = session.getClientReferenceId();
            if (orderId != null && !orderId.isEmpty()) {
                log.info("Payment approved! Order ID: {}", orderId);
                rabbitTemplate.convertAndSend("order-status-exchange", "order.paid", orderId);
            } else {
                log.warn("Webhook received but 'client_reference_id' is null.");
            }
        }
    }
}