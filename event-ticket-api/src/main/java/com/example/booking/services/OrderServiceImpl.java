package com.example.booking.services;

import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.request.order.CreateOrderRequest;
import com.example.booking.dto.OrderItemDto;
import com.example.booking.controller.response.order.OrdersResponse;
import com.example.booking.domain.entities.*;
import com.example.booking.exception.OrderNotFoundException;
import com.example.booking.repositories.OrderRepository;
import com.example.booking.services.intefaces.OrderService;
import com.example.booking.services.intefaces.TicketService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

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

    @PreAuthorize("isAuthenticated()")
    public OrderItemDto createNewOrder(CreateOrderRequest dto) {
        UUID userId = jwtUtils.getAuthenticatedUserId();
        log.info("Creating new order for userId={}, ticketIds={}", userId, dto.ticketIds());

        User user = userService.findUserEntityById(userId);
        List<Ticket> tickets = ticketService.findAndValidateAvailableTickets(dto.ticketIds());

        var ticketOrder = new Order();
        ticketOrder.setTickets(new HashSet<>(tickets));

        ticketOrder.setUser(user);

        Order savedOrder = orderRepository.save(ticketOrder);
        log.info("Order created successfully. orderId={}, userId={}, totalPrice={}", savedOrder.getOrderId(), user.getUserId(), ticketOrder.getOrderPrice());

        updateTicketAssociations(tickets, savedOrder);
        evictCaches(tickets);


        return new OrderItemDto(
                savedOrder.getOrderId(),
                savedOrder.getOrderPrice(),
                tickets.stream().map(Ticket::toTicketItemDto).collect(Collectors.toList()),
                user.getUserId()
        );
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public OrdersResponse getOrdersByUserId(UUID userId, int page, int pageSize) {
        String cacheKey = userId + "-" + page + "-" + pageSize;

        var cache = cacheManager.getCache(CacheNames.ORDERS);

        if (cache != null) {
            OrdersResponse cachedValue = cache.get(cacheKey, OrdersResponse.class);
            if (cachedValue != null) {
                log.info("Cache HIT for key={}", cacheKey);
                return cachedValue;
            }
        }

        log.info("Cache MISS. Fetching orders for userId={}, page={}, pageSize={}", userId, page, pageSize);

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var ordersPage = orderRepository
                .findOrdersByUserIdWithAssociations(userId, pageRequest)
                .map(Order::toOrderItemDto);

        OrdersResponse response = new OrdersResponse(
                ordersPage.getContent(),
                page,
                pageSize,
                ordersPage.getTotalPages(),
                ordersPage.getTotalElements()
        );

        if (cache != null) {
            cache.put(cacheKey, response);
            log.info("Stored result in cache for key={}", cacheKey);
        }

        return response;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOrderOwner(#orderId)")
    public void deleteOrder(UUID orderId) {
        log.info("Attempting to delete orderId={}", orderId);

        Order order = orderRepository.findByIdWithFullAssociations(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id={}", orderId);
                    return new OrderNotFoundException();
                });

        restoreStock(order.getTickets());

        evictCaches(new ArrayList<>(order.getTickets()));

        orderRepository.deleteById(orderId);
        log.info("Order deleted successfully. orderId={}", orderId);
    }

    private void restoreStock(Set<Ticket> tickets) {
        tickets.forEach(ticket -> {
            ticket.getTicketCategory().incrementTicketCategory();
            ticket.getEvent().incrementAvailableTickets();

            ticket.setOrder(null);
        });
    }

    private void updateTicketAssociations(List<Ticket> tickets, Order savedOrder) {
        tickets.forEach(ticket -> ticket.setOrder(savedOrder));
    }

    private void evictCaches(List<Ticket> tickets) {
        Objects.requireNonNull(cacheManager.getCache(CacheNames.ORDERS)).clear();

        tickets.stream()
                .map(t -> t.getEvent().getEventId())
                .distinct()
                .forEach(eventId -> {
                    Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(eventId);
                    log.info("Evicted REMAINING_TICKETS cache for eventId={}", eventId);
                });
    }
}
