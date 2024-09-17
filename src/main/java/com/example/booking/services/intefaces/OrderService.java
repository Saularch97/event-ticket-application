package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.CreateNewOrderDto;
import com.example.booking.controller.dto.OrderItemDto;
import com.example.booking.controller.dto.OrdersDto;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public interface OrderService {
    OrderItemDto createNewOrder(CreateNewOrderDto dto, JwtAuthenticationToken token);
    OrdersDto getUserOrders(int page, int pageSize, JwtAuthenticationToken token);
    void deleteOrder(UUID orderId);
}
