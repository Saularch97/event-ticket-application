package com.example.booking.entities;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "tb_orders")
public class Order {

    public Order() {
    }

    public Order(UUID orderId, Double orderPrice, List<Ticket> tickets, User user) {
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

    @OneToMany(cascade = CascadeType.ALL, mappedBy="order", fetch = FetchType.LAZY)
    private List<Ticket> tickets = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.ALL)
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

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public Double calculateOrderPrice(List<Ticket> tickets) {

        Double totalPrice = 0.0;

        for (Ticket t : tickets) {
            totalPrice += t.getTicketPrice();
        }

        return totalPrice;
    }
}

