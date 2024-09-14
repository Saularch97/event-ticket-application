package com.example.booking.entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "tb_ticket_orders")
public class TicketOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "order_id")
    private UUID orderId;

//    @ManyToOne(cascade = CascadeType.ALL)
//    @JoinColumn(name = "ticket_id")
//    private Ticket ticket;
}
