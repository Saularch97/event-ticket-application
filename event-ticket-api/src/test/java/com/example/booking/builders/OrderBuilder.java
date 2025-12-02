package com.example.booking.builders;

import com.example.booking.domain.entities.Order;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.User;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class OrderBuilder {

    private UUID orderId;
    private BigDecimal orderPrice;
    private Set<Ticket> tickets = new HashSet<>();
    private User user;

    public OrderBuilder() {
    }

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder withOrderId(UUID orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderBuilder withOrderPrice(BigDecimal orderPrice) {
        this.orderPrice = orderPrice;
        return this;
    }

    public OrderBuilder withTickets(Set<Ticket> tickets) {
        if (tickets != null) {
            this.tickets = tickets;
        }
        return this;
    }

    public OrderBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public Order build() {
        Order order = new Order();
        order.setOrderId(this.orderId);
        order.setOrderPrice(this.orderPrice);
        order.setTickets(this.tickets);
        order.setUser(this.user);
        return order;
    }
}
