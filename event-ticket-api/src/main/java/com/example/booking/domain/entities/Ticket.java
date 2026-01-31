package com.example.booking.domain.entities;

import com.example.booking.domain.enums.ETicketStatus;
import com.example.booking.dto.TicketItemDto;
import com.example.booking.exception.InvalidTicketValidationCodeException;
import com.example.booking.exception.TicketAlreadyHaveAnOrderException;
import com.example.booking.exception.TicketAlreadyUsedException;
import com.example.booking.exception.TicketNotPaidException;
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
    private LocalDateTime emittedAt;

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

    @Column(name = "validation_code")
    private String validationCode;

    protected Ticket() {
    }

    public static Ticket build(User owner, Event event, TicketCategory category) {
        if (event == null || category == null) {
            throw new IllegalArgumentException("Event and category are obligatory to create a ticket!");
        }

        Ticket ticket = new Ticket();

        ticket.ticketOwner = owner;
        ticket.event = event;
        ticket.ticketCategory = category;

        ticket.ticketCategoryName = category.getName();
        ticket.ticketPrice = category.getPrice();
        ticket.ticketEventLocation = event.getEventLocation();
        ticket.ticketEventDate = event.getEventDate().toString();
        ticket.emittedAt = LocalDateTime.now();

        ticket.ticketStatus = ETicketStatus.PENDING;

        return ticket;
    }

    public void performCheckIn(String validationCodeInput) {
        if (this.validationCode == null || !this.validationCode.equalsIgnoreCase(validationCodeInput)) {
            throw new InvalidTicketValidationCodeException("Invalid validation code for ticket " + this.ticketId);
        }

        if (this.ticketStatus == ETicketStatus.USED) {
            throw new TicketAlreadyUsedException("Ticket already used!");
        }

        if (this.ticketStatus != ETicketStatus.PAID) {
            throw new TicketNotPaidException("Ticket is not paid. Status: " + this.ticketStatus);
        }

        this.ticketStatus = ETicketStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public String regenerateValidationCode() {
        if (this.ticketStatus == ETicketStatus.USED) {
            throw new TicketAlreadyUsedException("Ticket already used. Cannot generate new QR Code.");
        }

        this.validationCode = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return this.validationCode;
    }

    public void reserve(Order order) {
        if (this.order != null) {
            throw new TicketAlreadyHaveAnOrderException("This ticket is already reserved/sold.");
        }

        if (this.ticketStatus == ETicketStatus.USED || this.ticketStatus == ETicketStatus.EXPIRED) {
            throw new TicketAlreadyUsedException("Cannot reserve a ticket that is used or expired.");
        }

        this.order = order;
        this.ticketStatus = ETicketStatus.PENDING;
    }

    public void removeOrderAssociation() {
        this.order = null;
        this.ticketStatus = ETicketStatus.PENDING; // Volta a ficar disponível (ou EXPIRED dependendo da regra)
    }

    public boolean isPaid() {
        return this.ticketStatus == ETicketStatus.PAID;
    }

    public void expire() {
        this.ticketStatus = ETicketStatus.EXPIRED;
        this.order = null;
    }

    public void markAsPaid() {
        this.ticketStatus = ETicketStatus.PAID;
    }

    public void markAsPending()  {
        this.ticketStatus = ETicketStatus.PENDING;
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

    public UUID getTicketId() {
        return ticketId;
    }

    private void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public Event getEvent() {
        return event;
    }

    private void setEvent(Event event) {
        this.event = event;
    }

    public User getTicketOwner() {
        return ticketOwner;
    }

    private void setTicketOwner(User ticketOwner) {
        this.ticketOwner = ticketOwner;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public TicketCategory getTicketCategory() {
        return ticketCategory;
    }

    private void setTicketCategory(TicketCategory ticketCategory) {
        this.ticketCategory = ticketCategory;
    }

    public LocalDateTime getEmittedAt() {
        return emittedAt;
    }

    private void setEmittedAt(LocalDateTime emittedAt) {
        this.emittedAt = emittedAt;
    }

    public String getTicketEventLocation() {
        return ticketEventLocation;
    }

    private void setTicketEventLocation(String ticketEventLocation) {
        this.ticketEventLocation = ticketEventLocation;
    }

    public String getTicketEventDate() {
        return ticketEventDate;
    }

    private void setTicketEventDate(String ticketEventDate) {
        this.ticketEventDate = ticketEventDate;
    }

    public String getTicketCategoryName() {
        return ticketCategoryName;
    }

    private void setTicketCategoryName(String ticketCategoryName) {
        this.ticketCategoryName = ticketCategoryName;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    private void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public ETicketStatus getTicketStatus() {
        return ticketStatus;
    }

    private void setTicketStatus(ETicketStatus ticketStatus) {
        this.ticketStatus = ticketStatus;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    private void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public String getValidationCode() {
        return validationCode;
    }

    private void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }
}
