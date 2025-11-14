package com.example.booking.services;


import com.example.booking.builders.EventBuilder;
import com.example.booking.builders.OrderBuilder;
import com.example.booking.builders.TicketBuilder;
import com.example.booking.builders.TicketCategoryBuilder;
import com.example.booking.config.cache.CacheNames;
import com.example.booking.controller.request.order.CreateOrderRequest;
import com.example.booking.controller.response.order.OrdersResponse;
import com.example.booking.domain.entities.*;
import com.example.booking.dto.OrderItemDto;
import com.example.booking.dto.TicketItemDto;
import com.example.booking.exception.OrderNotFoundException;
import com.example.booking.exception.TicketAlreadyHaveAnOrderException;
import com.example.booking.exception.TicketNotFoundException;
import com.example.booking.repositories.OrderRepository;
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
    private static final String SORT_PROPERTY_ORDER_PRICE = "orderPrice";
    private static final double TICKET_PRICE_1 = 100.0;
    private static final double TICKET_PRICE_2 = 150.0;
    private static final double TOTAL_ORDER_PRICE = TICKET_PRICE_1 + TICKET_PRICE_2;
    private static final double MOCK_ORDER_PRICE = 100.0;
    private static final Long MOCK_CATEGORY_ID = 1L;
    private static final int SINGLE_ITEM_COUNT = 1;
    private static final long SINGLE_ITEM_COUNT_LONG = 1L;

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

        Event event = EventBuilder.anEvent().withEventId(eventId).build();
        TicketCategory tc1 = TicketCategoryBuilder.aTicketCategory().withPrice(TICKET_PRICE_1).build();
        TicketCategory tc2 = TicketCategoryBuilder.aTicketCategory().withPrice(TICKET_PRICE_2).build();

        Ticket ticket1 = TicketBuilder.aTicket()
                .withTicketId(ticketId1)
                .withTicketCategory(tc1)
                .withEvent(event)
                .withOrder(null)
                .withTicketOwner(user)
                .build();

        Ticket ticket2 = TicketBuilder.aTicket()
                .withTicketId(ticketId2)
                .withTicketCategory(tc2)
                .withEvent(event)
                .withOrder(null)
                .withTicketOwner(user)
                .build();

        List<Ticket> tickets = List.of(ticket1, ticket2);

        Order savedOrder = new Order();
        savedOrder.setOrderId(UUID.randomUUID());
        savedOrder.setUser(user);
        savedOrder.setTickets(new HashSet<>(tickets));
        savedOrder.setOrderPrice(TOTAL_ORDER_PRICE);

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(userService.findUserEntityById(userId)).thenReturn(user);
        when(ticketService.findTicketsWithEventDetails(request.ticketIds())).thenReturn(tickets);
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

        verify(remainingTicketsCache, times(tickets.size())).evict(eventId);
        verify(ordersCache).clear();
    }

    @Test
    void createNewOrder_shouldThrowTicketNotFoundException_whenTicketListSizeMismatch() {
        UUID ticketId1 = UUID.randomUUID();
        UUID ticketId2 = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(List.of(ticketId1, ticketId2));

        Ticket ticket1 = TicketBuilder.aTicket().withTicketId(ticketId1).build();
        List<Ticket> foundTickets = List.of(ticket1);

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(userService.findUserEntityById(userId)).thenReturn(user);
        when(ticketService.findTicketsWithEventDetails(request.ticketIds())).thenReturn(foundTickets);

        assertThatThrownBy(() -> orderServiceImpl.createNewOrder(request))
                .isInstanceOf(TicketNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(cacheManager, never()).getCache(anyString());
    }

    @Test
    void createNewOrder_shouldThrowTicketAlreadyHaveAnOrderException() {
        UUID ticketId1 = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(List.of(ticketId1));

        Ticket ticket1 = TicketBuilder.aTicket().withTicketId(ticketId1).withOrder(OrderBuilder.anOrder().build()).build();
        List<Ticket> tickets = List.of(ticket1);

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(userService.findUserEntityById(userId)).thenReturn(user);
        when(ticketService.findTicketsWithEventDetails(request.ticketIds())).thenReturn(tickets);

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

        when(jwtUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(ordersCache.get(cacheKey, OrdersResponse.class)).thenReturn(cachedResponse);

        OrdersResponse result = orderServiceImpl.getUserOrders(page, pageSize);

        assertThat(result).isSameAs(cachedResponse);
        verify(orderRepository, never()).findOrdersByUserIdWithAssociations(any(), any());
        verify(ordersCache, never()).put(any(), any());
    }

    @Test
    void getUserOrders_shouldFetchFromRepository_whenCacheMiss() {
        int page = DEFAULT_PAGE;
        int pageSize = DEFAULT_PAGE_SIZE;
        String cacheKey = userId + "-" + page + "-" + pageSize;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.Direction.ASC, SORT_PROPERTY_ORDER_PRICE);

        UUID orderId = UUID.randomUUID();
        double orderPrice = MOCK_ORDER_PRICE;
        UUID ticketId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ticketUserId = UUID.randomUUID();
        Long categoryId = MOCK_CATEGORY_ID;

        Order orderMock = mock(Order.class);
        Ticket ticketMock = mock(Ticket.class);
        Event eventMock = mock(Event.class);
        TicketCategory categoryMock = mock(TicketCategory.class);
        User ticketDtoUserMock = mock(User.class);

        when(orderMock.getOrderId()).thenReturn(orderId);
        when(orderMock.getOrderPrice()).thenReturn(orderPrice);
        when(orderMock.getUser()).thenReturn(user);
        when(orderMock.getTickets()).thenReturn(Set.of(ticketMock));

        when(ticketMock.getTicketId()).thenReturn(ticketId);
        when(ticketMock.getEvent()).thenReturn(eventMock);
        when(eventMock.getEventId()).thenReturn(eventId);
        when(ticketMock.getTicketOwner()).thenReturn(ticketDtoUserMock);
        when(ticketDtoUserMock.getUserId()).thenReturn(ticketUserId);
        when(ticketMock.getTicketCategory()).thenReturn(categoryMock);
        when(categoryMock.getTicketCategoryId()).thenReturn(categoryId);

        Page<Order> ordersPage = new PageImpl<>(List.of(orderMock), pageRequest, SINGLE_ITEM_COUNT_LONG);
        when(jwtUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(ordersCache.get(cacheKey, OrdersResponse.class)).thenReturn(null);
        when(orderRepository.findOrdersByUserIdWithAssociations(userId, pageRequest)).thenReturn(ordersPage);

        OrdersResponse result = orderServiceImpl.getUserOrders(page, pageSize);

        assertThat(result).isNotNull();
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.pageSize()).isEqualTo(pageSize);
        assertThat(result.totalPages()).isEqualTo(SINGLE_ITEM_COUNT);
        assertThat(result.totalElements()).isEqualTo(SINGLE_ITEM_COUNT_LONG);

        assertThat(result.orders()).hasSize(SINGLE_ITEM_COUNT);

        OrderItemDto resultOrderDto = result.orders().getFirst();
        assertThat(resultOrderDto.orderId()).isEqualTo(orderId);
        assertThat(resultOrderDto.orderPrice()).isEqualTo(orderPrice);
        assertThat(resultOrderDto.userid()).isEqualTo(userId);

        assertThat(resultOrderDto.tickets()).hasSize(SINGLE_ITEM_COUNT);
        TicketItemDto resultTicketDto = resultOrderDto.tickets().getFirst();
        assertThat(resultTicketDto.ticketId()).isEqualTo(ticketId);
        assertThat(resultTicketDto.eventId()).isEqualTo(eventId);
        assertThat(resultTicketDto.userId()).isEqualTo(ticketUserId);
        assertThat(resultTicketDto.ticketCategoryId()).isEqualTo(categoryId);

        verify(ordersCache).put(cacheKeyCaptor.capture(), ordersResponseCaptor.capture());
        assertThat(cacheKeyCaptor.getValue()).isEqualTo(cacheKey);
        assertThat(ordersResponseCaptor.getValue()).isEqualTo(result);
    }

    @Test
    void deleteOrder_shouldDeleteOrderSuccessfully() {
        UUID orderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        Event event = mock(Event.class);
        when(event.getEventId()).thenReturn(eventId);

        TicketCategory ticketCategory = mock(TicketCategory.class);

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
}
