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
import com.example.booking.services.intefaces.OrderService;
import com.example.booking.services.intefaces.TicketService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    public OrderItemDto createNewOrder(CreateOrderRequest dto) {
        UUID userId = jwtUtils.getAuthenticatedUserId();
        log.info("Creating new order for userId={}, ticketIds={}", userId, dto.ticketIds());

        User user = userService.findUserEntityById(userId);
        List<Ticket> tickets = ticketService.findTicketsWithEventDetails(dto.ticketIds());

        if (tickets.size() != dto.ticketIds().size()) {
            log.warn("Some tickets not found. Expected={}, Found={}", dto.ticketIds().size(), tickets.size());
            throw new TicketNotFoundException();
        }

        for (Ticket ticket : tickets) {
            if (ticket.getOrder() != null) {
                log.warn("Ticket with ID={} already has an associated order", ticket.getTicketId());
                throw new TicketAlreadyHaveAnOrderException(ticket.getTicketId() + " not found for!");
            }
        }

        var ticketOrder = new Order();
        ticketOrder.setTickets(new HashSet<>(tickets));

        double orderPrice = 0.0;
        for (Ticket ticket : tickets) {
            orderPrice += ticket.getTicketCategory().getPrice();
        }

        ticketOrder.setOrderPrice(orderPrice);
        ticketOrder.setUser(user);

        Order savedOrder = orderRepository.save(ticketOrder);
        log.info("Order created successfully. orderId={}, userId={}, totalPrice={}", savedOrder.getOrderId(), user.getUserId(), orderPrice);

        tickets.forEach(ticket -> {
            ticket.setOrder(savedOrder);
            UUID eventId = ticket.getEvent().getEventId();
            Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(eventId);
            log.debug("Evicted REMAINING_TICKETS cache for eventId={} in method createNewOrder", eventId);
        });

        Objects.requireNonNull(cacheManager.getCache(CacheNames.ORDERS)).clear();
        log.debug("Cleared ORDERS cache after new order creation");

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
        log.info("Fetching orders for userId={}, page={}, pageSize={}", userId, page, pageSize);

        Cache cache = cacheManager.getCache(CacheNames.ORDERS);
        Objects.requireNonNull(cache, "Orders cache not configured!");

        String cacheKey = userId + "-" + page + "-" + pageSize;
        OrdersResponse cachedOrders = cache.get(cacheKey, OrdersResponse.class);

        if (cachedOrders != null) {
            log.debug("Returning orders from cache. cacheKey={}", cacheKey);
            return cachedOrders;
        }

        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        var ordersPage = orderRepository
                .findOrdersByUserIdWithAssociations(userId, pageRequest)
                .map(Order::toOrderItemDto);

        OrdersResponse resultToCache = new OrdersResponse(
                ordersPage.getContent(),
                page,
                pageSize,
                ordersPage.getTotalPages(),
                ordersPage.getTotalElements()
        );

        cache.put(cacheKey, resultToCache);
        log.debug("Cached user orders. cacheKey={}", cacheKey);

        return resultToCache;
    }

    @Transactional
    public void deleteOrder(UUID orderId) {
        log.info("Attempting to delete orderId={}", orderId);

        Order order = orderRepository.findByIdWithFullAssociations(orderId)
                .orElseThrow(() -> {
                    log.warn("Order not found with id={}", orderId);
                    return new OrderNotFoundException();
                });

        for (Ticket ticket : order.getTickets()) {
            Event event = ticket.getEvent();
            ticket.getTicketCategory().incrementTicketCategory();
            event.incrementAvailableTickets();

            UUID eventId = event.getEventId();
            Objects.requireNonNull(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).evict(eventId);
            log.debug("Evicted REMAINING_TICKETS cache for eventId={}", eventId);
        }

        Objects.requireNonNull(cacheManager.getCache(CacheNames.ORDERS)).clear();
        log.debug("Cleared ORDERS cache after deleting orderId={}", orderId);

        orderRepository.deleteById(orderId);
        log.info("Order deleted successfully. orderId={}", orderId);
    }
}
