package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import com.example.booking.controller.request.EmmitTicketRequest;

import java.util.UUID;

public interface TicketsService {
    TicketItemDto emmitTicket(EmmitTicketRequest request, String token);
    void deleteEmittedTicket(UUID ticketId, String token);
    TicketsDto listAllUserTickets(String token, int page, int pageSize);
    public TicketsDto listAllTickets(int page, int pageSize);
}
