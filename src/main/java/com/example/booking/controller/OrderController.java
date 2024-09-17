package com.example.booking.controller;

import com.example.booking.entities.dto.CreateNewOrderDto;
import com.example.booking.entities.dto.OrderItemDto;
import com.example.booking.entities.dto.OrdersDto;
import com.example.booking.services.OrderService;
import com.example.booking.util.UriUtil;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Transactional
    @PostMapping("/order")
    public ResponseEntity<OrderItemDto> createNewOrder(
            @RequestBody CreateNewOrderDto dto,
            JwtAuthenticationToken token
    ) throws Exception {

        var savedOrder = orderService.createNewOrder(dto, token);

        URI location = UriUtil.getUriLocation("orderId", savedOrder.orderId());

        return ResponseEntity.created(location).body(savedOrder);
    }

    @GetMapping("/ordersByUser")
    public ResponseEntity<OrdersDto> getUserOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                   JwtAuthenticationToken token) throws Exception {

        return ResponseEntity.ok(orderService.getUserOrders(page, pageSize, token));
    }
}
