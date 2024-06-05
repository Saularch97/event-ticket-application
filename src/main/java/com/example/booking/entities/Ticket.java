package com.example.booking.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_tickets")
public class Ticket {

    public Ticket() {
    }

    public Ticket(UUID ticketId, User ticketOwner, String ticketName, String eventLocation, LocalDateTime eventDate, Double ticketPrice) {
        this.ticketId = ticketId;
        this.ticketOwner = ticketOwner;
        this.ticketName = ticketName;
        this.eventLocation = eventLocation;
        this.eventDate = eventDate;
        this.ticketPrice = ticketPrice;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id")
    private UUID ticketId;


    // TODO olhar, pois cada ingresso é único, então 1 : 1
    @ManyToOne(cascade = CascadeType.ALL)
    private User ticketOwner;
    private String ticketName;

    private String eventLocation;
    private LocalDateTime eventDate;
    private Double ticketPrice;

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public User getTicketOwner() {
        return ticketOwner;
    }

    public void setTicketOwner(User ticketOwner) {
        this.ticketOwner = ticketOwner;
    }

    public String getTicketName() {
        return ticketName;
    }

    public void setTicketName(String ticketName) {
        this.ticketName = ticketName;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(Double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "ticketId=" + ticketId +
                ", ticketOwner=" + ticketOwner +
                ", ticketName='" + ticketName + '\'' +
                ", eventLocation='" + eventLocation + '\'' +
                ", eventDate=" + eventDate +
                ", ticketPrice=" + ticketPrice +
                '}';
    }
}
