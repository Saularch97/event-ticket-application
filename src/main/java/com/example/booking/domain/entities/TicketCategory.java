package com.example.booking.domain.entities;

import com.example.booking.controller.dto.TicketCategoryDto;
import jakarta.persistence.*;

@Entity
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

    public static TicketCategoryDto toTicketCategoryDto(TicketCategory ticketCategory) {
        return new TicketCategoryDto(
                ticketCategory.getName(),
                ticketCategory.getPrice()
        );
    }
}

