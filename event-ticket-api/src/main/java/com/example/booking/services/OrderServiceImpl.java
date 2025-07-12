package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.request.order.CreateOrderRequest;
import com.example.booking.dto.OrderItemDto;
import com.example.booking.controller.response.order.OrdersResponse;
import com.example.booking.domain.entities.*;
import com.example.booking.exception.OrderNotFoundException;
import com.example.booking.exception.TicketAlreadyHaveAnOrderException;
import com.example.booking.exception.TicketNotFoundException;
import com.example.booking.repositories.OrderRepository;
import com.example.booking.repositories.UserRepository;
import com.example.booking.services.intefaces.OrderService;
import com.example.booking.services.intefaces.TicketService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final TicketService ticketService;
    private final JwtUtils jwtUtils;
    private final CacheManager cacheManager;

    public OrderServiceImpl(OrderRepository orderRepository, UserService userService, TicketService ticketService, JwtUtils jwtUtils, CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.userService = userService;
        this.ticketService = ticketService;
        this.jwtUtils = jwtUtils;
        this.cacheManager = cacheManager;
    }

    public OrderItemDto createNewOrder(CreateOrderRequest dto) {

        UUID userId = jwtUtils.getAuthenticatedUserId();
        User user = userService.findUserEntityById(userId);

        List<Ticket> tickets = ticketService.findTicketsWithEventDetails(dto.ticketIds());

        if (tickets.size() != dto.ticketIds().size()) {
            throw new TicketNotFoundException();
        }

        for (Ticket ticket : tickets) {
            if (ticket.getOrder() != null) {
                throw new TicketAlreadyHaveAnOrderException(ticket.getTicketId() + " not found for!");
            }
        }

        var ticketOrder = new Order();
        ticketOrder.setTickets(new HashSet<>(tickets));

        var orderPrice = 0.0;

        for (Ticket ticket : tickets) {
            orderPrice += ticket.getTicketCategory().getPrice();
        }

        ticketOrder.setOrderPrice(orderPrice);
        ticketOrder.setUser(user);

        Order savedOrder = orderRepository.save(ticketOrder);

        tickets.forEach(ticket -> {
            ticket.setOrder(savedOrder);
            Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(ticket.getEvent().getEventId());
        });

        Objects.requireNonNull(cacheManager.getCache(CacheNames.ORDERS)).clear();

        return new OrderItemDto(
                savedOrder.getOrderId(),
                savedOrder.getOrderPrice(),
                tickets.stream().map(Ticket::toTicketItemDto).collect(Collectors.toList()),
                user.getUserId()
        );
    }

    @Override
    public OrdersResponse getUserOrders(int page, int pageSize) {
        UUID userId = jwtUtils.getAuthenticatedUserId();

        Cache cache = cacheManager.getCache(CacheNames.ORDERS);
        Objects.requireNonNull(cache, "Orders cache not configured!");

        String cacheKey = userId.toString() + "-" + page + "-" + pageSize;

        OrdersResponse cachedOrders = cache.get(cacheKey, OrdersResponse.class);

        if (cachedOrders != null) {
            return cachedOrders;
        }

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var ordersPage = orderRepository
                .findOrdersByUserIdWithAssociations(userId, pageRequest)
                .map(Order::toOrderItemDto);

        OrdersResponse resultToCache = new OrdersResponse(ordersPage.getContent(), page, pageSize, ordersPage.getTotalPages(), ordersPage.getTotalElements());

        cache.put(cacheKey, resultToCache);

        return resultToCache;
    }

    @Transactional
    public void deleteOrder(UUID orderId) {

        Order order = orderRepository.findByIdWithFullAssociations(orderId)
                .orElseThrow(OrderNotFoundException::new);

        for (Ticket ticket: order.getTickets()) {
            Event event = ticket.getEvent();
            Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(event.getEventId());
            event.incrementAvailableTickets();
            ticket.getTicketCategory().incrementTicketCategory();
        }

        Objects.requireNonNull(cacheManager.getCache(CacheNames.ORDERS)).clear();

        orderRepository.deleteById(orderId);
    }
}
