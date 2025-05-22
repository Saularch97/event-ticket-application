package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.request.CreateOrderRequest;
import com.example.booking.controller.dto.OrderItemDto;
import com.example.booking.controller.dto.OrdersDto;
import com.example.booking.domain.entities.*;
import com.example.booking.repository.TicketOrderRepository;
import com.example.booking.repository.TicketRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.OrderService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private final TicketOrderRepository ticketOrderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final JwtUtils jwtUtils;
    private final CacheManager cacheManager;

    public OrderServiceImpl(TicketOrderRepository ticketOrderRepository, UserRepository userRepository, TicketRepository ticketRepository, JwtUtils jwtUtils, CacheManager cacheManager) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.jwtUtils = jwtUtils;
        this.cacheManager = cacheManager;
    }

    public OrderItemDto createNewOrder(CreateOrderRequest dto, String token) {

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
            orderPrice += ticket.getEvent().getEventTicketPrice();
        }

        ticketOrder.setOrderPrice(orderPrice);
        ticketOrder.setUser(user.get());

        Order savedOrder = ticketOrderRepository.save(ticketOrder);

        tickets.forEach(ticket -> ticket.setOrder(savedOrder));
        ticketRepository.saveAll(tickets);

        savedOrder.getTickets().forEach(ticket -> {
            Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(ticket.getEvent().getEventId());
        });

        return Order.toOrderItemDto(savedOrder);
    }

    /*
    @Cacheable(
            value = CacheNames.ORDERS,
            key = "{#userName}"
    )
     */
    public OrdersDto getUserOrders(int page, int pageSize, String userName) {

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var orders = ticketOrderRepository
                .findOrderByUserName(userName, pageRequest)
                .map(Order::toOrderItemDto);

        return new OrdersDto(orders.getContent(), page, pageSize, orders.getTotalPages(), orders.getTotalElements());
    }

    @Transactional
    public void deleteOrder(UUID orderId, String token) {

        var order = ticketOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        for (var ticket: order.getTickets()) {
            ticket.getEvent().incrementAvailableTickets();
            ticket.getTicketCategory().incrementTicketCategory();
        }

        ticketOrderRepository.deleteById(orderId);
    }

}
