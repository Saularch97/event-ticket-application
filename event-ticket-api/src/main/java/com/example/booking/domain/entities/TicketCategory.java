package com.example.booking.domain.entities;

import com.example.booking.controller.dto.TicketCategoryDto;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_ticket_category")
public class TicketCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_category_id")
    private Integer ticketCategoryId;

    private String name;
    private Double price;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "available_category_tickets")
    private Integer availableCategoryTickets;

    public TicketCategory() {
    }

    public TicketCategory(Integer availableCategoryTickets, Event event, Double price, String name, Integer ticketCategoryId) {
        this.availableCategoryTickets = availableCategoryTickets;
        this.event = event;
        this.price = price;
        this.name = name;
        this.ticketCategoryId = ticketCategoryId;
    }

    public Integer getTicketCategoryId() {
        return ticketCategoryId;
    }

    public void setTicketCategoryId(Integer ticketCategoryId) {
        this.ticketCategoryId = ticketCategoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Integer getAvailableCategoryTickets() {
        return availableCategoryTickets;
    }

    public void setAvailableCategoryTickets(Integer availableCategoryTickets) {
        this.availableCategoryTickets = availableCategoryTickets;
    }

    public void decrementTicketCategory() {
        if (this.availableCategoryTickets <= 0) {
            throw new IllegalStateException("No more tickets available.");
        }
        this.availableCategoryTickets -= 1;
    }

    public void incrementTicketCategory() {
        this.availableCategoryTickets += 1;
    }

    public static TicketCategoryDto toTicketCategoryDto(TicketCategory ticketCategory) {
        return new TicketCategoryDto(
                ticketCategory.getName(),
                ticketCategory.getPrice(),
                ticketCategory.getAvailableCategoryTickets()
        );
    }
}

