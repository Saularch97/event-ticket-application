package com.booking.paymentprocessor.services;

import com.booking.paymentprocessor.configuration.RabbitMQConfig;
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
    private final static String PAYMENT_SUCCESS = "checkout.session.completed";
    private final static String PAYMENT_EXPIRED = "checkout.session.expired";

    public WebhookServiceImpl(RabbitTemplate rabbitTemplate,
                              @Value("${stripe.webhook.secret}") String endpointSecret) {
        this.rabbitTemplate = rabbitTemplate;
        this.endpointSecret = endpointSecret;
    }

    @Override
    public void handleWebhook(String payload, String sigHeader) throws SignatureVerificationException, EventDataObjectDeserializationException {

        Event event = Webhook.constructEvent(payload, sigHeader, endpointSecret);

        switch (event.getType()) {
            case PAYMENT_SUCCESS:
                processPaymentSuccess(event);
                break;
            case PAYMENT_EXPIRED:
                processPaymentFailure(event);
                break;
            default:
                log.debug("Event ignored: {}", event.getType());
        }
    }

    private void processPaymentSuccess(Event event) throws EventDataObjectDeserializationException {
        Session session = deserializeSession(event);
        if (session != null) {
            String orderId = session.getClientReferenceId();
            if (isValidOrderId(orderId)) {
                log.info("Payment approved! Order ID: {}", orderId);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_STATUS_EXCHANGE,
                        RabbitMQConfig.ORDER_PAID_ROUTING_KEY,
                        orderId
                );
            }
        }
    }

    private void processPaymentFailure(Event event) throws EventDataObjectDeserializationException {
        Session session = deserializeSession(event);
        if (session != null) {
            String orderId = session.getClientReferenceId();
            if (isValidOrderId(orderId)) {
                log.warn("Payment session expired/failed for Order ID: {}. Triggering compensation.", orderId);

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ORDER_STATUS_EXCHANGE,
                        RabbitMQConfig.PAYMENT_FAILED_ROUTING_KEY,
                        orderId
                );
            }
        }
    }

    private Session deserializeSession(Event event) throws EventDataObjectDeserializationException {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        StripeObject stripeObject = null;

        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            stripeObject = dataObjectDeserializer.deserializeUnsafe();
        }

        if (stripeObject instanceof Session) {
            return (Session) stripeObject;
        }

        if (stripeObject != null) {
            log.warn("Event received for an object that is not an session: {}", stripeObject.getClass().getName());
        }

        return null;
    }

    private boolean isValidOrderId(String orderId) {
        if (orderId != null && !orderId.isEmpty()) {
            return true;
        }
        log.warn("Webhook received but 'client_reference_id' is null/empty.");
        return false;
    }
}