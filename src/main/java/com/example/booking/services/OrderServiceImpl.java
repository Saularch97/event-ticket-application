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
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

    public OrderServiceImpl(TicketOrderRepository ticketOrderRepository, UserRepository userRepository, TicketRepository ticketRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
    }

    // TODO no futuro deixar a intenção de compra order cacheada no redis
    public OrderItemDto createNewOrder(CreateNewOrderDto dto, JwtAuthenticationToken token) {
        Optional<User> user = userRepository.findById(UUID.fromString(token.getName()));
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

        return savedOrder.toOrderItemDto();
    }

    public OrdersDto getUserOrders(int page, int pageSize, JwtAuthenticationToken token) {

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var orders = ticketOrderRepository
                .findOrderByUserId(UUID.fromString(token.getName()), pageRequest)
                .map(Order::toOrderItemDto);

        return new OrdersDto(orders.getContent(), page, pageSize, orders.getTotalPages(), orders.getTotalElements());
    }

    @Override
    public void deleteOrder(UUID orderId) {
        // TODO implement
    }
}
