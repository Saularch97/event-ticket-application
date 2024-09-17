package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.OrderTicketDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public interface TicketsService {
    TicketItemDto orderTicket(OrderTicketDto dto, JwtAuthenticationToken token);
    void deleteTicketOrder(UUID ticketId, JwtAuthenticationToken token);
    TicketsDto listAllUserTickets(JwtAuthenticationToken token, int page, int pageSize);
}
