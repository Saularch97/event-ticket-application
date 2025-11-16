package com.example.booking.config.security;

import com.example.booking.repositories.OrderRepository;
import com.example.booking.util.JwtUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("orderSecurity")
public class OrderSecurity {

    private final OrderRepository orderRepository;
    private final JwtUtils jwtUtils;

    public OrderSecurity(OrderRepository orderRepository, JwtUtils jwtUtils) {
        this.orderRepository = orderRepository;
        this.jwtUtils = jwtUtils;
    }

    public boolean isOrderOwner(UUID orderId) {
        UUID authenticatedUser = jwtUtils.getAuthenticatedUserId();

        return orderRepository.findById(orderId)
                .map(order -> order.getUser().getUserId().equals(authenticatedUser))
                .orElse(false);
    }
}
