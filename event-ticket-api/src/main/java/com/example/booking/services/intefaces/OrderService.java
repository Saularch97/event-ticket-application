package com.example.booking.services.intefaces;

import com.example.booking.controller.request.order.CreateOrderRequest;
import com.example.booking.dto.OrderItemDto;
import com.example.booking.controller.response.order.OrdersResponse;

import java.util.UUID;

public interface OrderService {
    OrderItemDto createNewOrder(CreateOrderRequest dto);
    OrdersResponse getOrdersByUserId(UUID userId, int page, int pageSize);
    void deleteOrder(UUID orderId);
    void updateOrderStatusToPaid(UUID orderId);
}
