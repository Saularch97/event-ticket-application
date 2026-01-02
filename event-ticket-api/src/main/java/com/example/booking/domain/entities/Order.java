package com.example.booking.domain.entities;

import com.example.booking.domain.enums.EOrderStatus;
import com.example.booking.dto.OrderItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_orders")
public class Order {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "order_price", precision = 19, scale = 2)
    private BigDecimal orderPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy="order", fetch = FetchType.LAZY)
    private Set<Ticket> tickets = new HashSet<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "order_status")
    @Enumerated(EnumType.STRING)
    private EOrderStatus orderStatus;

    public Order() {
    }

    public Order(UUID orderId, BigDecimal orderPrice, Set<Ticket> tickets, User user, EOrderStatus orderStatus) {
        this.orderId = orderId;
        this.orderPrice = orderPrice;
        this.tickets = tickets;
        this.user = user;
        this.orderStatus = orderStatus;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
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

    public EOrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(EOrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public static OrderItemDto toOrderItemDto(Order order) {
        return new OrderItemDto(
                order.getOrderId(),
                order.getOrderPrice(),
                order.getTickets().stream().map(Ticket::toTicketItemDto).collect(Collectors.toList()),
                order.getUser().getUserId(),
                null
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