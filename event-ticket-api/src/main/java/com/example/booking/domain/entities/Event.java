package com.example.booking.domain.entities;

import com.example.booking.dto.EventItemDto;
import com.example.booking.dto.EventSummaryDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "tb_events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "event_location")
    private String eventLocation;
    @Column(name = "event_name")
    private String eventName;
    @Column(name = "event_date")
    private LocalDateTime eventDate;
    // TODO limar coluna eventTicketPrice
    @Column(name = "event_ticket_price")
    private Double eventTicketPrice;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "event", orphanRemoval = true)
    private List<TicketCategory> ticketCategories = new ArrayList<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "event", fetch = FetchType.LAZY)
    private Set<Ticket> tickets;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User eventOwner;

    @Column(name = "available_tickets")
    private Integer availableTickets;

    @Column(name = "original_amount_of_tickets")
    private Integer originalAmountOfTickets;

    @Column(name = "is_trending")
    private Boolean isTrending;

    @Column(name = "tickets_emitted_in_trending_period")
    private Long ticketsEmittedInTrendingPeriod = 0L;

    public Event() {
    }

    public Event(UUID eventId,
                 String eventLocation,
                 String eventName,
                 LocalDateTime eventDate,
                 Set<Ticket> tickets,
                 User eventOwner,
                 Double eventTicketPrice,
                 Integer availableTickets,
                 List<TicketCategory> ticketCategories,
                 Boolean isTrending,
                 Long ticketsEmittedInTrendingPeriod) {
        this.eventId = eventId;
        this.eventLocation = eventLocation;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.tickets = tickets;
        this.eventOwner = eventOwner;
        this.eventTicketPrice = eventTicketPrice;
        this.availableTickets = availableTickets;
        this.ticketCategories = ticketCategories;
        this.isTrending = isTrending;
        this.ticketsEmittedInTrendingPeriod = ticketsEmittedInTrendingPeriod;
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

    public Double getEventTicketPrice() {
        return eventTicketPrice;
    }

    public void setEventTicketPrice(Double eventTicketPrice) {
        this.eventTicketPrice = eventTicketPrice;
    }

    public Integer getAvailableTickets() {
        return availableTickets;
    }

    public void setAvailableTickets(Integer availableTickets) {
        this.availableTickets = availableTickets;
        if (this.originalAmountOfTickets == null) {
            this.originalAmountOfTickets = availableTickets;
        }
    }

    public List<TicketCategory> getTicketCategories() {
        return ticketCategories;
    }

    public void setTicketCategories(List<TicketCategory> ticketCategories) {
        this.ticketCategories = ticketCategories;
    }

    public Integer getOriginalAmountOfTickets() {
        return originalAmountOfTickets;
    }

    public void setOriginalAmountOfTickets(Integer originalAmountOfTickets) {
        this.originalAmountOfTickets = originalAmountOfTickets;
    }

    public Boolean getTrending() {
        return isTrending;
    }

    public void setTrending(Boolean trending) {
        isTrending = trending;
    }

    public Long getTicketsEmittedInTrendingPeriod() {
        return ticketsEmittedInTrendingPeriod;
    }

    public void setTicketsEmittedInTrendingPeriod(Long ticketsEmittedInTrendingPeriod) {
        this.ticketsEmittedInTrendingPeriod = ticketsEmittedInTrendingPeriod;
    }

    public static EventItemDto toEventItemDto(Event event) {
        return new EventItemDto(
                event.getEventId(),
                event.getEventName(),
                event.getEventDate().toString(),
                event.getEventDate().getHour(),
                event.getEventDate().getMinute(),
                event.getEventTicketPrice(),
                event.getAvailableTickets(),
                event.getTicketCategories().stream().map(TicketCategory::toTicketCategoryDto).toList()
        );
    }

    public static EventSummaryDto toEventSummaryDto(Event event) {
        return new EventSummaryDto(
                event.getEventId(),
                event.getEventName(),
                event.getEventLocation(),
                event.getAvailableTickets(),
                event.getEventDate()
        );
    }

    public void decrementAvailableTickets() {
        if (this.availableTickets <= 0) {
            throw new IllegalStateException("No more tickets available.");
        }
        this.availableTickets -= 1;
    }

    public void incrementAvailableTickets() {
        this.availableTickets += 1;
    }
}
