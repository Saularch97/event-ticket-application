package com.example.booking.messaging.consumer;


import com.example.booking.config.RabbitMQConfig;
import com.example.booking.services.intefaces.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentConfirmationConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationConsumer.class);
    private final OrderService orderService;

    public PaymentConfirmationConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_PAID_QUEUE)
    public void handlePaymentConfirmation(String orderId) {
        log.info("Message received from queue with confirmation of order {}", orderId);

        try {
            orderService.updateOrderStatusToPaid(UUID.fromString(orderId));
        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage());
            throw e;
        }
    }
}
