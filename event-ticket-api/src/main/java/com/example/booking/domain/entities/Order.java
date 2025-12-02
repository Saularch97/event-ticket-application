package com.example.booking.domain.entities;

import com.example.booking.dto.OrderItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "order_price", precision = 19, scale = 2)
    private BigDecimal orderPrice;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy="order", fetch = FetchType.LAZY)
    private Set<Ticket> tickets = new HashSet<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Order() {
    }

    public Order(UUID orderId, BigDecimal orderPrice, Set<Ticket> tickets, User user) {
        this.orderId = orderId;
        this.orderPrice = orderPrice;
        this.tickets = tickets;
        this.user = user;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(BigDecimal orderPrice) {
        this.orderPrice = orderPrice;
    }

    public Set<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(Set<Ticket> tickets) {
        this.tickets = tickets;
        this.orderPrice = calculateTotal(tickets);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public static OrderItemDto toOrderItemDto(Order order) {
        return new OrderItemDto(
                order.getOrderId(),
                order.getOrderPrice(),
                order.getTickets().stream().map(Ticket::toTicketItemDto).collect(Collectors.toList()),
                order.getUser().getUserId()
        );
    }

    private BigDecimal calculateTotal(Set<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return tickets.stream()
                .map(t -> t.getTicketCategory().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

