package com.example.booking.domain.entities;

import com.example.booking.domain.enums.ETicketStatus;
import com.example.booking.dto.TicketItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_tickets")
public class Ticket {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
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

    @Column(name = "ticket_event_location")
    private String ticketEventLocation;

    @Column(name = "ticket_event_date")
    private String ticketEventDate;

    @Column(name = "ticket_category_name")
    private String ticketCategoryName;

    @Column(name = "ticket_price", precision = 19, scale = 2)
    private BigDecimal ticketPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ETicketStatus ticketStatus;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Ticket(UUID ticketId,
                  Event event,
                  User ticketOwner,
                  Order order,
                  TicketCategory ticketCategory,
                  LocalDateTime emittedAt,
                  String ticketEventLocation,
                  String ticket,
                  String ticketCategoryName,
                  BigDecimal ticketPrice) {
        this.ticketId = ticketId;
        this.event = event;
        this.ticketOwner = ticketOwner;
        this.order = order;
        this.ticketCategory = ticketCategory;
        this.emittedAt = emittedAt;
        this.ticketEventLocation = ticketEventLocation;
        this.ticketEventDate = ticket;
        this.ticketCategoryName = ticketCategoryName;
        this.ticketPrice = ticketPrice;
    }

    public Ticket() {
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

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getTicketCategoryName() {
        return ticketCategoryName;
    }

    public void setTicketCategoryName(String ticketCategoryName) {
        this.ticketCategoryName = ticketCategoryName;
    }

    public String getTicketEventDate() {
        return ticketEventDate;
    }

    public void setTicketEventDate(String ticketEventDate) {
        this.ticketEventDate = ticketEventDate;
    }

    public String getTicketEventLocation() {
        return ticketEventLocation;
    }

    public void setTicketEventLocation(String ticketEventLocation) {
        this.ticketEventLocation = ticketEventLocation;
    }

    public ETicketStatus getTicketStatus() {
        return ticketStatus;
    }

    public void setTicketStatus(ETicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public static TicketItemDto toTicketItemDto(Ticket ticket) {
        return new TicketItemDto(
                ticket.getTicketId(),
                ticket.getEvent().getEventId(),
                ticket.getTicketOwner().getUserId(),
                ticket.getTicketCategory().getTicketCategoryId(),
                ticket.getTicketPrice()
        );
    }
}
