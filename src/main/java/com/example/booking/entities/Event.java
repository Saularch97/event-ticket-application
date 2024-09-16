package com.example.booking.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;
    private String eventLocation;
    private String eventName;
    private LocalDateTime eventDate;
    @OneToMany
    private Set<Ticket> tickets;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User eventOwner;

    public Event() {
    }

    public Event(UUID eventId, String eventLocation, String eventName, LocalDateTime eventDate, Set<Ticket> tickets, User eventOwner) {
        this.eventId = eventId;
        this.eventLocation = eventLocation;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.tickets = tickets;
        this.eventOwner = eventOwner;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Set<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(Set<Ticket> tickets) {
        this.tickets = tickets;
    }

    public User getEventOwner() {
        return eventOwner;
    }

    public void setEventOwner(User eventOwner) {
        this.eventOwner = eventOwner;
    }
}
