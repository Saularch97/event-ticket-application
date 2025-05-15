package com.example.booking.domain.entities;

import com.example.booking.controller.dto.TicketItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User ticketOwner;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_category_id")
    private TicketCategory ticketCategory;

    @Column(name = "emitted_at")
    private LocalDateTime emittedAt = LocalDateTime.now();

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Ticket(UUID ticketId, Event event, User ticketOwner, TicketCategory ticketCategory) {
        this.ticketId = ticketId;
        this.event = event;
        this.ticketOwner = ticketOwner;
        this.ticketCategory = ticketCategory;
    }

    public Ticket() {
    }

    public LocalDateTime getEmittedAtAt() {
        return emittedAt;
    }

    public void setEmittedAtAt(LocalDateTime emittedAt) {
        this.emittedAt = emittedAt;
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

    public LocalDateTime getEmittedAt() {
        return emittedAt;
    }

    public void setEmittedAt(LocalDateTime emittedAt) {
        this.emittedAt = emittedAt;
    }

    public TicketCategory getTicketCategory() {
        return ticketCategory;
    }

    public void setTicketCategory(TicketCategory ticketCategory) {
        this.ticketCategory = ticketCategory;
    }

    public static TicketItemDto toTicketItemDto(Ticket ticket) {
        return new TicketItemDto(
                ticket.getTicketId(),
                Event.toEventItemDto(ticket.getEvent()),
                User.toUserDto(ticket.getTicketOwner()),
                TicketCategory.toTicketCategoryDto(ticket.getTicketCategory())
        );
    }
}
