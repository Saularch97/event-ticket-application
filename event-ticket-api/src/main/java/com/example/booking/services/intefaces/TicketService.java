package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.RemainingTicketCategoryDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.controller.request.EmmitTicketRequest;
import com.example.booking.domain.entities.Ticket;

import java.util.List;
import java.util.UUID;

public interface TicketService {
    TicketItemDto emmitTicket(EmmitTicketRequest request);
    void deleteEmittedTicket(UUID ticketId);
    TicketsDto listAllUserTickets(int page, int pageSize);
    TicketsDto listAllTickets(int page, int pageSize);
    List<RemainingTicketCategoryDto> getAvailableTicketsByCategoryFromEvent(UUID eventId);
    TicketsDto getTicketsByCategoryId(Integer categoryId, int page, int pageSize);
    List<Ticket> findTicketsWithEventDetails(List<UUID> ticketIds);
}
