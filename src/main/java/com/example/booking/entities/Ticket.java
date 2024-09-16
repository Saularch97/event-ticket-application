package com.example.booking.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id")
    private UUID ticketId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "event_id")
    private Event event;
    private Double ticketPrice;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User ticketOwner;

    public Ticket(UUID ticketId, Event event, Double ticketPrice, User ticketOwner) {
        this.ticketId = ticketId;
        this.event = event;
        this.ticketPrice = ticketPrice;
        this.ticketOwner = ticketOwner;
    }

    public Ticket() {
    }

    public User getTicketOwner() {
        return ticketOwner;
    }

    public void setTicketOwner(User ticketOwner) {
        this.ticketOwner = ticketOwner;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public Double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(Double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }
}
