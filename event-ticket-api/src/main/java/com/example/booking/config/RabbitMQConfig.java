package com.example.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EVENT_QUEUE = "event-request-queue";
    public static final String EVENT_EXCHANGE = "event-request-exchange";
    public static final String EVENT_RK = "event-request-queue-key";

    public static final String ORDER_PAID_QUEUE = "order-paid-queue";
    public static final String ORDER_STATUS_EXCHANGE = "order-status-exchange";
    public static final String ORDER_PAID_RK = "order.paid";

    public static final String DLQ_QUEUE = "booking-dlq";
    public static final String DLQ_EXCHANGE = "booking-dlx";

    public static final String PAYMENT_FAILED_QUEUE = "payment-failed-queue";
    public static final String PAYMENT_FAILED_RK = "payment.failed";

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLQ_EXCHANGE);
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    @Bean
    Queue eventRequestQueue() {
        return QueueBuilder.durable(EVENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_QUEUE)
                .build();
    }

    @Bean
    DirectExchange eventRequestExchange() {
        return new DirectExchange(EVENT_EXCHANGE);
    }

    @Bean
    Binding eventRequestBinding() {
        return BindingBuilder.bind(eventRequestQueue()).to(eventRequestExchange()).with(EVENT_RK);
    }

    // ... (Seus Beans de Order Paid mantidos) ...

    @Bean
    Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_QUEUE)
                .build();
    }

    @Bean
    TopicExchange orderStatusExchange() {
        return new TopicExchange(ORDER_STATUS_EXCHANGE);
    }

    @Bean
    Binding orderPaidBinding() {
        return BindingBuilder.bind(orderPaidQueue()).to(orderStatusExchange()).with(ORDER_PAID_RK);
    }

    @Bean
    Queue paymentFailedQueue() {
        return QueueBuilder.durable(PAYMENT_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_QUEUE)
                .build();
    }

    @Bean
    Binding paymentFailedBinding() {
        return BindingBuilder
                .bind(paymentFailedQueue())
                .to(orderStatusExchange())
                .with(PAYMENT_FAILED_RK);
    }
}
