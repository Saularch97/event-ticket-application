package com.booking.paymentprocessor.configuration;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_STATUS_EXCHANGE = "order-status-exchange";

    public static final String ORDER_PAID_QUEUE = "order-paid-queue";

    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";

    @Bean
    public TopicExchange orderStatusExchange() {
        return new TopicExchange(ORDER_STATUS_EXCHANGE);
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE).build();
    }

    @Bean
    public Binding bindingOrderPaid(Queue orderPaidQueue, TopicExchange orderStatusExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderStatusExchange).with(ORDER_PAID_ROUTING_KEY);
    }
}
