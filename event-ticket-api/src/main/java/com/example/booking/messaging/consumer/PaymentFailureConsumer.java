package com.example.booking.messaging.consumer;

import com.example.booking.services.intefaces.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentFailureConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentFailureConsumer.class);
    private final OrderService orderService;

    public PaymentFailureConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = "payment.failed.queue")
    public void handlePaymentFailure(String orderId) {
        try {
            log.warn("Payment failed for order {}. Executing compensation transaction...", orderId);

            orderService.cancelOrderAndReleaseTicket(UUID.fromString(orderId));

            log.info("Compensation successful. Order {} cancelled and stock released.", orderId);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to compensate order {}. Stock might be stuck.", orderId, e);
            throw e;
        }
    }
}
