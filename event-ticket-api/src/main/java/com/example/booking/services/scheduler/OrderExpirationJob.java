package com.example.booking.services.scheduler;

import com.example.booking.domain.entities.Order;
import com.example.booking.domain.enums.EOrderStatus;
import com.example.booking.domain.enums.ETicketStatus;
import com.example.booking.repositories.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderExpirationJob {
    private static final Logger log = LoggerFactory.getLogger(OrderExpirationJob.class);

    private final OrderRepository orderRepository;

    public OrderExpirationJob(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cleanupExpiredOrders() {
        LocalDateTime limit = LocalDateTime.now().minusMinutes(30);
        List<Order> expiredOrders = orderRepository.findAllByOrderStatusAndCreatedAtBefore(
                EOrderStatus.PENDING_PAYMENT, limit);

        for (Order order : expiredOrders) {
            order.setOrderStatus(EOrderStatus.CANCELED);

            order.getTickets().forEach(ticket -> {
                ticket.setTicketStatus(ETicketStatus.EXPIRED);
                ticket.setOrder(null);
            });

            orderRepository.save(order);
            log.info("Order {} expired", order.getOrderId());
        }
    }
}