package com.example.booking.entities;

import com.example.booking.controller.dto.EventItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private Double eventPrice;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "event", fetch = FetchType.LAZY)
    private Set<Ticket> tickets;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User eventOwner;

    public Event() {
    }

    public Event(UUID eventId, String eventLocation, String eventName, LocalDateTime eventDate, Set<Ticket> tickets, User eventOwner, Double eventPrice) {
        this.eventId = eventId;
        this.eventLocation = eventLocation;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.tickets = tickets;
        this.eventOwner = eventOwner;
        this.eventPrice = eventPrice;
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

    public Double getEventPrice() {
        return eventPrice;
    }

    public void setEventPrice(Double eventPrice) {
        this.eventPrice = eventPrice;
    }

    public EventItemDto toEventItemDto() {
        return new EventItemDto(
                eventId,
                eventName,
                eventDate,
                eventDate.getHour(),
                eventDate.getMinute(),
                eventPrice
        );
    }
}
