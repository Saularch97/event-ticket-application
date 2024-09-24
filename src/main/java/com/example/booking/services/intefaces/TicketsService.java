package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.OrderTicketDto;
import com.example.booking.controller.dto.TicketItemDto;
import com.example.booking.controller.dto.TicketsDto;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public interface TicketsService {
    TicketItemDto orderTicket(OrderTicketDto dto, String token);
    void deleteTicketOrder(UUID ticketId, String token);
    TicketsDto listAllUserTickets(String token, int page, int pageSize);
}
