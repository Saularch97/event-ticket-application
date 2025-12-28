package com.example.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    private static final String EVENT_QUEUE = "event-request-queue";
    private static final String EVENT_EXCHANGE = "event-request-exchange";
    private static final String EVENT_RK = "event-request-queue-key";

    @Value("${rabbitmq.payment.queue}")
    private String paymentQueueName;

    @Value("${rabbitmq.payment.exchange}")
    private String paymentExchangeName;

    @Value("${rabbitmq.payment.routing-key}")
    private String paymentRoutingKey;

    @Bean Queue eventRequestQueue() {
        return new Queue(EVENT_QUEUE, true);
    }

    @Bean DirectExchange eventRequestExchange() {
        return new DirectExchange(EVENT_EXCHANGE);
    }

    @Bean Binding eventRequestBinding() {
        return BindingBuilder.bind(eventRequestQueue()).to(eventRequestExchange()).with(EVENT_RK);
    }

    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable(paymentQueueName)
                .withArgument("x-dead-letter-exchange", paymentExchangeName + ".dlx")
                .withArgument("x-dead-letter-routing-key", paymentRoutingKey)
                .build();
    }

    @Bean
    DirectExchange paymentExchange() {
        return new DirectExchange(paymentExchangeName);
    }

    @Bean
    Binding paymentBinding() {
        return BindingBuilder.bind(paymentQueue())
                .to(paymentExchange())
                .with(paymentRoutingKey);
    }

    @Bean
    Queue paymentDLQ() {
        return new Queue(paymentQueueName + ".dlq", true);
    }

    @Bean
    DirectExchange paymentDLX() {
        return new DirectExchange(paymentExchangeName + ".dlx");
    }

    @Bean
    Binding paymentDLQBinding() {
        return BindingBuilder.bind(paymentDLQ()).to(paymentDLX()).with(paymentRoutingKey);
    }
}
