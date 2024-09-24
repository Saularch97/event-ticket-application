package com.example.booking.services;

import com.example.booking.controller.dto.CreateNewOrderDto;
import com.example.booking.controller.dto.OrderItemDto;
import com.example.booking.controller.dto.OrdersDto;
import com.example.booking.domain.entities.Order;
import com.example.booking.domain.entities.Ticket;
import com.example.booking.domain.entities.User;
import com.example.booking.repository.TicketOrderRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.OrderService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private final TicketOrderRepository ticketOrderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final JwtUtils jwtUtils;

    public OrderServiceImpl(TicketOrderRepository ticketOrderRepository, UserRepository userRepository, TicketRepository ticketRepository, JwtUtils jwtUtils) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.jwtUtils = jwtUtils;
    }

    // TODO no futuro deixar a intenção de compra order cacheada no redis
    // TODO testar pra ver se aceita dois ID`s repetidos de ingresso
    public OrderItemDto createNewOrder(CreateNewOrderDto dto, String token) {

        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        Optional<User> user = userRepository.findByUserName(userName);
        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");

        Set<Ticket> tickets = new HashSet<>();

        for (UUID ticketId : dto.ticketIds()) {
            var ticket = ticketRepository.findById(ticketId);
            if (ticket.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            if (ticket.get().getOrder() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket already has an order in it!");

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

        return Order.toOrderItemDto(savedOrder);
    }

    public OrdersDto getUserOrders(int page, int pageSize, String token) {

        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var orders = ticketOrderRepository
                .findOrderByUserName(userName, pageRequest)
                .map(Order::toOrderItemDto);

        return new OrdersDto(orders.getContent(), page, pageSize, orders.getTotalPages(), orders.getTotalElements());
    }

    @Override
    public void deleteOrder(UUID orderId) {
        // TODO implement
    }
}
