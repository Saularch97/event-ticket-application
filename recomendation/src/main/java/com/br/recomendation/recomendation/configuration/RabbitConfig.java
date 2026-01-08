package com.br.recomendation.recomendation.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private static final String QUEUE_NAME = "event-request-queue";
    private static final String EXCHANGE_NAME = "event-request-exchange";
    private static final String ROUTING_KEY = "event-request-queue-key";

    public static final String DLQ_QUEUE = "booking-dlq";
    public static final String DLQ_EXCHANGE = "booking-dlx";

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    Queue eventRequestQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_QUEUE)
                .build();
    }

    @Bean
    DirectExchange eventRequestExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    Binding eventRequestBinding() {
        return BindingBuilder.bind(eventRequestQueue())
                .to(eventRequestExchange())
                .with(ROUTING_KEY);
    }
}