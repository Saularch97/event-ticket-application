package com.example.booking.builders;


import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.TicketCategory;
import com.example.booking.domain.entities.User;

import java.time.LocalDateTime;
import java.util.*;

public class EventBuilder {

    // Campos que espelham a entidade Event
    private UUID eventId;
    private String eventLocation;
    private String eventName;
    private LocalDateTime eventDate;
    private List<TicketCategory> ticketCategories = new ArrayList<>();
    private Set<Ticket> tickets = new HashSet<>();
    private User eventOwner;
    private Integer availableTickets;
    private Integer originalAmountOfTickets;
    private Boolean isTrending;
    private Long ticketsEmittedInTrendingPeriod;


    public EventBuilder() {
    }


    public static EventBuilder anEvent() {
        return new EventBuilder();
    }


    public EventBuilder withEventId(UUID eventId) {
        this.eventId = eventId;
        return this;
    }

    public EventBuilder withEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
        return this;
    }

    public EventBuilder withEventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public EventBuilder withEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
        return this;
    }

    public EventBuilder withTicketCategories(List<TicketCategory> ticketCategories) {
        if (ticketCategories != null) {
            this.ticketCategories = ticketCategories;
        }
        return this;
    }

    public EventBuilder withTickets(Set<Ticket> tickets) {
        if (tickets != null) {
            this.tickets = tickets;
        }
        return this;
    }

    public EventBuilder withEventOwner(User eventOwner) {
        this.eventOwner = eventOwner;
        return this;
    }

    public EventBuilder withAvailableTickets(Integer availableTickets) {
        this.availableTickets = availableTickets;
        return this;
    }


    public EventBuilder withOriginalAmountOfTickets(Integer originalAmountOfTickets) {
        this.originalAmountOfTickets = originalAmountOfTickets;
        return this;
    }

    public EventBuilder withIsTrending(Boolean isTrending) {
        this.isTrending = isTrending;
        return this;
    }

    public EventBuilder withTicketsEmittedInTrendingPeriod(Long ticketsEmittedInTrendingPeriod) {
        this.ticketsEmittedInTrendingPeriod = ticketsEmittedInTrendingPeriod;
        return this;
    }


    public Event build() {
        Event event = new Event();

        event.setEventId(this.eventId);
        event.setEventLocation(this.eventLocation);
        event.setEventName(this.eventName);
        event.setEventDate(this.eventDate);
        event.setTickets(this.tickets);
        event.setEventOwner(this.eventOwner);
        event.setTicketCategories(this.ticketCategories);
        event.setTrending(this.isTrending);

        event.setAvailableTickets(this.availableTickets);

        if (this.originalAmountOfTickets != null) {
            event.setOriginalAmountOfTickets(this.originalAmountOfTickets);
        }

        if (this.ticketsEmittedInTrendingPeriod != null) {
            event.setTicketsEmittedInTrendingPeriod(this.ticketsEmittedInTrendingPeriod);
        }

        return event;
    }
}