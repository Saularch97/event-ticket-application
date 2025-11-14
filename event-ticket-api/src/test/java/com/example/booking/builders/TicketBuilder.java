package com.example.booking.builders;

import com.example.booking.domain.entities.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class TicketBuilder {

    private UUID ticketId;
    private Event event;
    private User ticketOwner;
    private Order order;
    private TicketCategory ticketCategory;
    private LocalDateTime emittedAt;
    private String ticketEventLocation;
    private String ticketEventDate;
    private String ticketCategoryName;
    private Double ticketPrice;

    public TicketBuilder() {
    }

    public static TicketBuilder aTicket() {
        return new TicketBuilder();
    }

    public TicketBuilder withTicketId(UUID ticketId) {
        this.ticketId = ticketId;
        return this;
    }

    public TicketBuilder withEvent(Event event) {
        this.event = event;
        return this;
    }

    public TicketBuilder withTicketOwner(User ticketOwner) {
        this.ticketOwner = ticketOwner;
        return this;
    }

    public TicketBuilder withOrder(Order order) {
        this.order = order;
        return this;
    }

    public TicketBuilder withTicketCategory(TicketCategory ticketCategory) {
        this.ticketCategory = ticketCategory;
        return this;
    }

    public TicketBuilder withEmittedAt(LocalDateTime emittedAt) {
        this.emittedAt = emittedAt;
        return this;
    }

    public TicketBuilder withTicketEventLocation(String ticketEventLocation) {
        this.ticketEventLocation = ticketEventLocation;
        return this;
    }

    public TicketBuilder withTicketEventDate(String ticketEventDate) {
        this.ticketEventDate = ticketEventDate;
        return this;
    }

    public TicketBuilder withTicketCategoryName(String ticketCategoryName) {
        this.ticketCategoryName = ticketCategoryName;
        return this;
    }

    public TicketBuilder withTicketPrice(Double ticketPrice) {
        this.ticketPrice = ticketPrice;
        return this;
    }

    public Ticket build() {
        Ticket ticket = new Ticket();
        ticket.setTicketId(this.ticketId);
        ticket.setEvent(this.event);
        ticket.setTicketOwner(this.ticketOwner);
        ticket.setOrder(this.order);
        ticket.setTicketCategory(this.ticketCategory);
        ticket.setTicketEventLocation(this.ticketEventLocation);
        ticket.setTicketEventDate(this.ticketEventDate);
        ticket.setTicketCategoryName(this.ticketCategoryName);
        ticket.setTicketPrice(this.ticketPrice);

        if (this.emittedAt != null) {
            ticket.setEmittedAt(this.emittedAt);
        }

        return ticket;
    }
}
