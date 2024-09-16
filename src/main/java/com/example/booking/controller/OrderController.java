package com.example.booking.controller;

import com.example.booking.controller.dto.*;
import com.example.booking.entities.Ticket;
import com.example.booking.entities.Order;
import com.example.booking.entities.User;
import com.example.booking.repository.TicketOrderRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.util.UriUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

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
    @PostMapping("/order")
    public ResponseEntity<OrderItemDto> createNewOrder(
            @RequestBody CreateNewOrderDto dto,
            JwtAuthenticationToken token
    ) throws Exception {
        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));
        if (user.isEmpty()) throw new Exception("User not exists!");

        List<Ticket> tickets = new ArrayList<>();

        // TODO adicionar lógica para não ter id`s repetidos
        for (UUID ticketId : dto.ticketIds()) {
            var ticket = ticketRepository.findById(ticketId);
            if (ticket.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (ticket.get().getOrder() != null) throw new Exception("Ticket already has an order in it!");

            tickets.add(ticket.get());
        }

        var ticketOrder = new Order();
        ticketOrder.setTickets(tickets);
        ticketOrder.setOrderPrice(ticketOrder.calculateOrderPrice(ticketOrder.getTickets()));
        ticketOrder.setUser(user.get());

        Order savedOrder = ticketOrderRepository.save(ticketOrder);

        for (Ticket ticket : tickets) {
            ticket.setOrder(savedOrder);
            ticketRepository.save(ticket);
        }

        OrderItemDto orderItemDto = new OrderItemDto(
                savedOrder.getOrderId(),
                savedOrder.getOrderPrice(),
                savedOrder.getTickets().stream().map(ticket ->
                        new TicketItemDto(
                                ticket.getTicketId(),
                                new EventItemDto(
                                        ticket.getEvent().getEventId(),
                                        ticket.getEvent().getEventName(),
                                        ticket.getEvent().getEventDate(),
                                        ticket.getEvent().getEventDate().getHour(),
                                        ticket.getEvent().getEventDate().getMinute()
                                ),
                                ticket.getTicketPrice(),
                                new UserDto(
                                        ticket.getTicketOwner().getUserId(),
                                        ticket.getTicketOwner().getUserName()
                                )
                        )
                ).collect(Collectors.toList()),
                new UserDto(
                        savedOrder.getUser().getUserId(),
                        savedOrder.getUser().getUserName()
                )
        );

        URI location = UriUtil.getUriLocation("orderId", savedOrder.getOrderId());

        return ResponseEntity.created(location).body(orderItemDto);
    }

    @GetMapping("/ordersByUser")
    public ResponseEntity<OrdersDto> getUserOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                   JwtAuthenticationToken token) throws Exception {

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var orders = ticketOrderRepository.findOrderByUserId(UUID.fromString(token.getName()), pageRequest)
                .map(order -> new OrderItemDto(order.getOrderId(),
                        order.getOrderPrice(),
                        order.getTickets().stream().map(ticket ->
                                new TicketItemDto(
                                        ticket.getTicketId(),
                                        new EventItemDto(
                                                ticket.getEvent().getEventId(),
                                                ticket.getEvent().getEventName(),
                                                ticket.getEvent().getEventDate(),
                                                ticket.getEvent().getEventDate().getHour(),
                                                ticket.getEvent().getEventDate().getMinute()
                                        ),
                                        ticket.getTicketPrice(),
                                        new UserDto(
                                                ticket.getTicketOwner().getUserId(),
                                                ticket.getTicketOwner().getUserName()
                                        ))
                        ).collect(Collectors.toList()),
                        new UserDto(
                                order.getUser().getUserId(),
                                order.getUser().getUserName()
                        )));

        return ResponseEntity.ok(new OrdersDto(orders.getContent(), page, pageSize, orders.getTotalPages(), orders.getTotalElements()));
    }
}
