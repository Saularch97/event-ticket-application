package com.example.booking.domain.entities;

import com.example.booking.controller.dto.OrderItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_orders")
public class Order {

    public Order() {
    }

    public Order(UUID orderId, Double orderPrice, Set<Ticket> tickets, User user) {
        this.orderId = orderId;
        this.orderPrice = orderPrice;
        this.tickets = tickets;
        this.user = user;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID orderId;
    private Double orderPrice;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy="order", fetch = FetchType.LAZY)
    private Set<Ticket> tickets = new HashSet<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Double getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(Double orderPrice) {
        this.orderPrice = orderPrice;
    }

    public Set<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(Set<Ticket> tickets) {
        this.tickets = tickets;
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
                User.toUserDto(order.getUser())
        );
    }
}

