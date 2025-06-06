package com.br.recomendation.recomendation.configuration;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private static final String QUEUE_NAME = "event-request-queue";
    private static final String EXCHANGE_NAME = "event-request-exchange";
    private static final String ROUTING_KEY = "event-request-queue-key";

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    Queue eventRequestQueue() {
        return new Queue(QUEUE_NAME, true, false, false);
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

    @Bean
    Queue eventRequestDLQ() {
        return new Queue(QUEUE_NAME + ".dlq", true);
    }

    @Bean
    DirectExchange eventRequestDLX() {
        return new DirectExchange(EXCHANGE_NAME + ".dlx");
    }

    @Bean
    Binding eventRequestDLQBinding() {
        return BindingBuilder.bind(eventRequestDLQ())
                            .to(eventRequestDLX())
                            .with(ROUTING_KEY);
    }

}