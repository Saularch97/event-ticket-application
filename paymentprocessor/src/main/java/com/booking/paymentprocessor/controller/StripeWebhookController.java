package com.booking.paymentprocessor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/webhooks")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public StripeWebhookController(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Falha na validação da assinatura do Webhook");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if ("checkout.session.completed".equals(event.getType())) {
            try {
                JsonNode rootNode = objectMapper.readTree(payload);
                JsonNode dataObject = rootNode.path("data").path("object");

                if (dataObject.has("client_reference_id") && !dataObject.get("client_reference_id").isNull()) {
                    String orderId = dataObject.get("client_reference_id").asText();

                    log.info("✅ PAGAMENTO APROVADO! ID do Pedido encontrado: {}", orderId);
                    rabbitTemplate.convertAndSend("order-status-exchange", "order.paid", orderId);
                } else {
                    log.error("⚠️ ALERTA: Webhook recebido, mas 'client_reference_id' veio NULO ou não existe.");
                    log.error("Verifique se o Front-end/Booking Service está enviando o ID do pedido na criação da sessão do Stripe.");
                }
            } catch (Exception e) {
                log.error("Erro ao processar JSON manualmente: {}", e.getMessage());
            }
        } else {
            log.debug("Evento ignorado: {}", event.getType());
        }

        return ResponseEntity.ok("");
    }
}