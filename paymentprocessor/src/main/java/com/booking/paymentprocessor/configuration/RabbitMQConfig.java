package com.booking.paymentprocessor.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_STATUS_EXCHANGE = "order-status-exchange";
    public static final String ORDER_PAID_QUEUE = "order-paid-queue";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    public static final String DLQ_QUEUE = "booking-dlq";
    public static final String DLQ_EXCHANGE = "booking-dlx";

    @Bean
    public TopicExchange orderStatusExchange() {
        return new TopicExchange(ORDER_STATUS_EXCHANGE);
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_QUEUE)
                .build();
    }

    @Bean
    public Binding bindingOrderPaid(Queue orderPaidQueue, TopicExchange orderStatusExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderStatusExchange).with(ORDER_PAID_ROUTING_KEY);
    }
}