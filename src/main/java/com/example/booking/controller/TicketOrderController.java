package com.example.booking.controller;

import com.example.booking.controller.dto.BuyTicketDto;
import com.example.booking.entities.Ticket;
import com.example.booking.entities.Order;
import com.example.booking.entities.User;
import com.example.booking.repository.TicketOrderRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class TicketOrderController {

    private final TicketOrderRepository ticketOrderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;


    public TicketOrderController(TicketOrderRepository ticketOrderRepository, UserRepository userRepository, TicketRepository ticketRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    @PostMapping("/orderTicket")
    public ResponseEntity<Void> buyTicket(
            @RequestBody BuyTicketDto dto,
            JwtAuthenticationToken token
    ) throws Exception {
        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));
        if (user.isEmpty()) throw new Exception("User not exists!");

        List <Ticket> tickets = ticketRepository.findAllById(dto.ticketIds());
        if(tickets.isEmpty()) throw new Exception("Ticket not exists!");

        var ticketOrder = new Order();
        ticketOrder.setTickets(new HashSet<>(tickets));
        ticketOrder.setOrderPrice(ticketOrder.calculateOrderPrice(ticketOrder.getTickets()));
        ticketOrder.setUser(user.get());

        ticketOrderRepository.save(ticketOrder);

        return ResponseEntity.ok().build();
    }
}
