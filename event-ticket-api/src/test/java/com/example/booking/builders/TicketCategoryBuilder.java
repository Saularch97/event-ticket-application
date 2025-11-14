package com.example.booking.builders;

import com.example.booking.domain.entities.Event;
import com.example.booking.domain.entities.TicketCategory;

public class TicketCategoryBuilder {

    private Long ticketCategoryId;
    private String name;
    private Double price;
    private Event event;
    private Integer availableCategoryTickets;


    public TicketCategoryBuilder() {
    }


    public static TicketCategoryBuilder aTicketCategory() {
        return new TicketCategoryBuilder();
    }


    public TicketCategoryBuilder withTicketCategoryId(Long ticketCategoryId) {
        this.ticketCategoryId = ticketCategoryId;
        return this;
    }

    public TicketCategoryBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TicketCategoryBuilder withPrice(Double price) {
        this.price = price;
        return this;
    }

    public TicketCategoryBuilder withEvent(Event event) {
        this.event = event;
        return this;
    }

    public TicketCategoryBuilder withAvailableCategoryTickets(Integer availableCategoryTickets) {
        this.availableCategoryTickets = availableCategoryTickets;
        return this;
    }


    public TicketCategory build() {
        TicketCategory ticketCategory = new TicketCategory();

        ticketCategory.setTicketCategoryId(this.ticketCategoryId);
        ticketCategory.setName(this.name);
        ticketCategory.setPrice(this.price);
        ticketCategory.setEvent(this.event);
        ticketCategory.setAvailableCategoryTickets(this.availableCategoryTickets);

        return ticketCategory;
    }
}
