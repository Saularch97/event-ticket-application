package com.example.booking.services;


import com.example.booking.builders.EventBuilder;
import com.example.booking.builders.OrderBuilder;
import com.example.booking.builders.TicketBuilder;
import com.example.booking.builders.TicketCategoryBuilder;
import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.request.order.CreateOrderRequest;
import com.example.booking.controller.response.order.OrdersResponse;
import com.example.booking.domain.entities.*;
import com.example.booking.domain.enums.EOrderStatus;
import com.example.booking.domain.enums.ETicketStatus;
import com.example.booking.dto.OrderItemDto;
import com.example.booking.dto.TicketItemDto;
import com.example.booking.exception.OrderNotFoundException;
import com.example.booking.exception.TicketAlreadyHaveAnOrderException;
import com.example.booking.exception.TicketNotFoundException;
import com.example.booking.repositories.OrderRepository;
import com.example.booking.services.client.PaymentClient;
import com.example.booking.services.intefaces.TicketService;
import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String TEST_USER_NAME = "Test User";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final BigDecimal TICKET_PRICE_1 = BigDecimal.valueOf(100);
    private static final BigDecimal TICKET_PRICE_2 = BigDecimal.valueOf(150);
    private static final BigDecimal TOTAL_ORDER_PRICE = TICKET_PRICE_1.add(TICKET_PRICE_2);
    private static final BigDecimal MOCK_ORDER_PRICE = BigDecimal.valueOf(100L);
    private static final Long MOCK_CATEGORY_ID = 1L;
    private static final String MOCK_CHECKOUT_URL = "https://checkout.stripe.com/pay/cs_test_123";

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserService userService;
    @Mock
    private TicketService ticketService;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache remainingTicketsCache;
    @Mock
    private Cache ordersCache;
    @Mock
    private PaymentClient paymentClient;

    @Captor
    private ArgumentCaptor<Order> orderArgumentCaptor;
    @Captor
    private ArgumentCaptor<String> cacheKeyCaptor;
    @Captor
    private ArgumentCaptor<OrdersResponse> ordersResponseCaptor;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setUserId(userId);
        user.setUserName(TEST_USER_NAME);

        lenient().when(cacheManager.getCache(CacheNames.REMAINING_TICKETS)).thenReturn(remainingTicketsCache);
        lenient().when(cacheManager.getCache(CacheNames.ORDERS)).thenReturn(ordersCache);
    }

    @Test
    void createNewOrder_shouldCreateNewOrderSuccessfully() {
        UUID ticketId1 = UUID.randomUUID();
        UUID ticketId2 = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(List.of(ticketId1, ticketId2));

        Event event = EventBuilder.anEvent().withEventId(eventId).withEventName("TestEvent").build();

        TicketCategory tc1 = TicketCategoryBuilder.aTicketCategory()
                .withPrice(TICKET_PRICE_1)
                .build();

        TicketCategory tc2 = TicketCategoryBuilder.aTicketCategory()
                .withPrice(TICKET_PRICE_2)
                .build();

        Ticket ticket1 = TicketBuilder.aTicket()
                .withTicketId(ticketId1)
                .withTicketCategory(tc1)
                .withEvent(event)
                .withTicketOwner(user)
                .withTicketPrice(TICKET_PRICE_1)
                .build();

        Ticket ticket2 = TicketBuilder.aTicket()
                .withTicketId(ticketId2)
                .withTicketCategory(tc2)
                .withEvent(event)
                .withTicketOwner(user)
                .withTicketPrice(TICKET_PRICE_2)
                .build();

        List<Ticket> tickets = List.of(ticket1, ticket2);

        Order savedOrder = new Order();
        savedOrder.setOrderId(UUID.randomUUID());
        savedOrder.setUser(user);
        savedOrder.setTickets(new HashSet<>(tickets));
        savedOrder.setOrderPrice(TOTAL_ORDER_PRICE);

        when(paymentClient.getCheckoutUrl(any())).thenReturn(MOCK_CHECKOUT_URL);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(userService.findUserEntityById(userId)).thenReturn(user);
        when(ticketService.findAndValidateAvailableTickets(request.ticketIds())).thenReturn(tickets);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderItemDto result = orderServiceImpl.createNewOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.orderId()).isEqualTo(savedOrder.getOrderId());
        assertThat(result.userid()).isEqualTo(userId);

        assertThat(result.orderPrice()).isEqualTo(TOTAL_ORDER_PRICE);
        assertThat(result.tickets()).hasSize(tickets.size());

        verify(orderRepository).save(orderArgumentCaptor.capture());
        Order capturedOrder = orderArgumentCaptor.getValue();

        assertThat(capturedOrder.getUser()).isEqualTo(user);
        assertThat(capturedOrder.getOrderPrice()).isEqualTo(TOTAL_ORDER_PRICE);
        assertThat(capturedOrder.getTickets()).containsExactlyInAnyOrder(ticket1, ticket2);

        assertThat(ticket1.getOrder()).isEqualTo(savedOrder);
        assertThat(ticket2.getOrder()).isEqualTo(savedOrder);

        verify(remainingTicketsCache, times(1)).evict(eventId);
        verify(ordersCache).clear();
    }

    @Test
    void createNewOrder_shouldThrowTicketNotFoundException_whenTicketListSizeMismatch() {
        UUID ticketId1 = UUID.randomUUID();
        UUID ticketId2 = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(List.of(ticketId1, ticketId2));

        when(ticketService.findAndValidateAvailableTickets(request.ticketIds()))
                .thenThrow(new TicketNotFoundException());

        assertThatThrownBy(() -> orderServiceImpl.createNewOrder(request))
                .isInstanceOf(TicketNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createNewOrder_shouldThrowTicketAlreadyHaveAnOrderException() {
        UUID ticketId1 = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(List.of(ticketId1));


        when(jwtUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(userService.findUserEntityById(userId)).thenReturn(user);

        when(ticketService.findAndValidateAvailableTickets(request.ticketIds()))
                .thenThrow(new TicketAlreadyHaveAnOrderException("Ticket is already sold"));

        assertThatThrownBy(() -> orderServiceImpl.createNewOrder(request))
                .isInstanceOf(TicketAlreadyHaveAnOrderException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getUserOrders_shouldReturnOrdersFromCache_whenCacheHit() {
        int page = DEFAULT_PAGE;
        int pageSize = DEFAULT_PAGE_SIZE;
        String cacheKey = userId + "-" + DEFAULT_PAGE + "-" + DEFAULT_PAGE_SIZE;

        OrdersResponse cachedResponse = new OrdersResponse(Collections.emptyList(), page, pageSize, 0, 0L);

        when(ordersCache.get(cacheKey, OrdersResponse.class)).thenReturn(cachedResponse);

        OrdersResponse result = orderServiceImpl.getOrdersByUserId(userId ,page, pageSize);

        assertThat(result).isSameAs(cachedResponse);
        verify(orderRepository, never()).findOrdersByUserIdWithAssociations(any(), any());
        verify(ordersCache, never()).put(any(), any());
    }

    @Test
    void getUserOrders_shouldFetchFromRepository_whenCacheMiss() {
        int page = DEFAULT_PAGE;
        int pageSize = DEFAULT_PAGE_SIZE;
        String cacheKey = userId + "-" + page + "-" + pageSize;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, "orderPrice");

        UUID orderId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketOwnerId = UUID.randomUUID();
        Long categoryId = MOCK_CATEGORY_ID;

        TicketCategory category = new TicketCategory();
        category.setTicketCategoryId(categoryId);
        category.setName("VIP");
        category.setPrice(TICKET_PRICE_1);

        Event event = new Event();
        event.setEventId(eventId);
        event.setEventName("Show de Teste");

        User ticketOwner = new User();
        ticketOwner.setUserId(ticketOwnerId);

        Ticket ticket = TicketBuilder.aTicket()
                .withTicketId(ticketId)
                .withEvent(event)
                .withTicketCategory(category)
                .withTicketOwner(ticketOwner)
                .withTicketPrice(TICKET_PRICE_1)
                .build();

        Order order = OrderBuilder.anOrder()
                .withOrderId(orderId)
                .withUser(user)
                .withOrderPrice(MOCK_ORDER_PRICE)
                .withTickets(Set.of(ticket))
                .build();

        ticket.setOrder(order);

        Page<Order> ordersPage = new PageImpl<>(List.of(order), pageRequest, 1);

        Cache cacheMock = mock(Cache.class);

        when(cacheManager.getCache(CacheNames.ORDERS)).thenReturn(cacheMock);

        when(cacheMock.get(cacheKey, OrdersResponse.class)).thenReturn(null);

        when(orderRepository.findOrdersByUserIdWithAssociations(userId, pageRequest)).thenReturn(ordersPage);

        OrdersResponse result = orderServiceImpl.getOrdersByUserId(userId, page, pageSize);

        OrderItemDto resultOrder = result.orders().getFirst();
        TicketItemDto resultTicket = resultOrder.tickets().getFirst();

        assertThat(result).isNotNull();
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.pageSize()).isEqualTo(pageSize);
        assertThat(result.totalElements()).isEqualTo(1L);

        assertThat(result.orders()).hasSize(1);

        assertThat(resultOrder.orderId()).isEqualTo(orderId);
        assertThat(resultOrder.orderPrice()).isEqualTo(MOCK_ORDER_PRICE);

        assertThat(resultOrder.tickets()).hasSize(1);

        assertThat(resultTicket.ticketId()).isEqualTo(ticketId);
        assertThat(resultTicket.eventId()).isEqualTo(eventId);
        assertThat(resultTicket.ticketCategoryId()).isEqualTo(categoryId);

        verify(cacheMock).put(cacheKeyCaptor.capture(), ordersResponseCaptor.capture());

        assertThat(cacheKeyCaptor.getValue()).isEqualTo(cacheKey);
        assertThat(ordersResponseCaptor.getValue()).usingRecursiveComparison().isEqualTo(result);

        verify(orderRepository).findOrdersByUserIdWithAssociations(userId, pageRequest);
    }

    @Test
    void deleteOrder_shouldDeleteOrderSuccessfully() {
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);
        when(event.getEventId()).thenReturn(eventId);

        TicketCategory ticketCategory = mock(TicketCategory.class);
        when(ticketCategory.getPrice()).thenReturn(BigDecimal.TEN);

        Ticket ticket = mock(Ticket.class);
        when(ticket.getEvent()).thenReturn(event);
        when(ticket.getTicketCategory()).thenReturn(ticketCategory);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setTickets(Set.of(ticket));

        when(orderRepository.findByIdWithFullAssociations(orderId)).thenReturn(Optional.of(order));

        orderServiceImpl.deleteOrder(orderId);

        verify(ticketCategory).incrementTicketCategory();
        verify(event).incrementAvailableTickets();

        verify(remainingTicketsCache).evict(eventId);
        verify(ordersCache).clear();

        verify(orderRepository).deleteById(orderId);
    }

    @Test
    void deleteOrder_shouldThrowOrderNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdWithFullAssociations(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderServiceImpl.deleteOrder(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(cacheManager, never()).getCache(anyString());
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void updateOrderStatusToPaid_shouldUpdateOrderAndTickets_whenStatusIsPending() {
        UUID orderId = UUID.randomUUID();

        TicketCategory dummyCategory = TicketCategoryBuilder.aTicketCategory()
                .withPrice(BigDecimal.TEN)
                .build();

        Ticket ticket1 = TicketBuilder.aTicket()
                .withTicketCategory(dummyCategory)
                .build();
        ticket1.setTicketStatus(ETicketStatus.PENDING);

        Ticket ticket2 = TicketBuilder.aTicket()
                .withTicketCategory(dummyCategory)
                .build();
        ticket2.setTicketStatus(ETicketStatus.PENDING);

        Order order = OrderBuilder.anOrder()
                .withOrderId(orderId)
                .withTickets(Set.of(ticket1, ticket2))
                .build();

        order.setOrderStatus(EOrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderServiceImpl.updateOrderStatusToPaid(orderId);

        assertThat(order.getOrderStatus()).isEqualTo(EOrderStatus.CONFIRMED);

        assertThat(ticket1.getTicketStatus()).isEqualTo(ETicketStatus.PAID);
        assertThat(ticket2.getTicketStatus()).isEqualTo(ETicketStatus.PAID);

        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatusToPaid_shouldThrowOrderNotFoundException_whenOrderDoesNotExist() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderServiceImpl.updateOrderStatusToPaid(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatusToPaid_shouldDoNothing_whenOrderIsNotPendingPayment() {
        UUID orderId = UUID.randomUUID();
        Order order = OrderBuilder.anOrder()
                .withOrderId(orderId)
                .build();

        order.setOrderStatus(EOrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderServiceImpl.updateOrderStatusToPaid(orderId);

        assertThat(order.getOrderStatus()).isEqualTo(EOrderStatus.CONFIRMED);

        verify(orderRepository, never()).save(any());
    }
}
