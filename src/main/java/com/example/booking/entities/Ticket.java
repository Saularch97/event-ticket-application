package com.example.booking.entities;

import com.example.booking.controller.dto.TicketItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "tb_tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id")
    private UUID ticketId;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "event_id")
    private Event event;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User ticketOwner;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private Order order;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Ticket(UUID ticketId, Event event, User ticketOwner) {
        this.ticketId = ticketId;
        this.event = event;

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

    public TicketItemDto toTicketItemDto() {
        return new TicketItemDto(
                ticketId,
                event.toEventItemDto(),
                ticketOwner.toUserDto()
        );
    }
}
