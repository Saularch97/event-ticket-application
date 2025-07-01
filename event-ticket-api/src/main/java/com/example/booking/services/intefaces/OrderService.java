package com.example.booking.services.intefaces;

import com.example.booking.controller.request.CreateOrderRequest;
import com.example.booking.controller.dto.OrderItemDto;
import com.example.booking.controller.dto.OrdersDto;

import java.util.UUID;

public interface OrderService {
    OrderItemDto createNewOrder(CreateOrderRequest dto);
    OrdersDto getUserOrders(int page, int pageSize);
    void deleteOrder(UUID orderId);
}
