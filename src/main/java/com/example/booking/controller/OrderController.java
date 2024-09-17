package com.example.booking.controller;

import com.example.booking.controller.dto.*;
import com.example.booking.entities.Ticket;
import com.example.booking.entities.Order;
import com.example.booking.entities.User;
import com.example.booking.repository.TicketOrderRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.util.UriUtil;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;

@RestController
public class OrderController {

    private final TicketOrderRepository ticketOrderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public OrderController(TicketOrderRepository ticketOrderRepository, UserRepository userRepository, TicketRepository ticketRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // TODO no futuro deixar a intenção de compra order cacheada no redis
    @Transactional
    @PostMapping("/order")
    public ResponseEntity<OrderItemDto> createNewOrder(
            @RequestBody CreateNewOrderDto dto,
            JwtAuthenticationToken token
    ) throws Exception {
        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));
        if (user.isEmpty()) throw new Exception("User not exists!");

        Set<Ticket> tickets = new HashSet<>();

        for (UUID ticketId : dto.ticketIds()) {
            var ticket = ticketRepository.findById(ticketId);
            if (ticket.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (ticket.get().getOrder() != null) throw new Exception("Ticket already has an order in it!");

            tickets.add(ticket.get());
        }

        var ticketOrder = new Order();
        ticketOrder.setTickets(new HashSet<>(tickets));

        var orderPrice = 0.0;

        for (Ticket ticket : tickets) {
            orderPrice += ticket.getEvent().getEventPrice();
        }

        ticketOrder.setOrderPrice(orderPrice);
        ticketOrder.setUser(user.get());

        Order savedOrder = ticketOrderRepository.save(ticketOrder);

        for (Ticket ticket : tickets) {
            ticket.setOrder(savedOrder);
            ticketRepository.save(ticket);
        }

        URI location = UriUtil.getUriLocation("orderId", savedOrder.getOrderId());

        return ResponseEntity.created(location).body(savedOrder.toOrderItemDto());
    }

    @GetMapping("/ordersByUser")
    public ResponseEntity<OrdersDto> getUserOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                   JwtAuthenticationToken token) throws Exception {

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var orders = ticketOrderRepository
                .findOrderByUserId(UUID.fromString(token.getName()), pageRequest)
                .map(Order::toOrderItemDto);

        return ResponseEntity.ok(new OrdersDto(orders.getContent(), page, pageSize, orders.getTotalPages(), orders.getTotalElements()));
    }
}
