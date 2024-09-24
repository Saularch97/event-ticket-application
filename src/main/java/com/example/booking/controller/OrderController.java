package com.example.booking.controller;

import com.example.booking.controller.dto.CreateNewOrderDto;
import com.example.booking.controller.dto.OrderItemDto;
import com.example.booking.controller.dto.OrdersDto;
import com.example.booking.services.OrderServiceImpl;
import com.example.booking.util.UriUtil;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;

    public OrderController(OrderServiceImpl orderServiceImpl) {
        this.orderServiceImpl = orderServiceImpl;
    }

    @Transactional
    @PostMapping("/order")
    public ResponseEntity<OrderItemDto> createNewOrder(
            @RequestBody CreateNewOrderDto dto,
            @RequestHeader(name = "Cookie") String token
    ) throws Exception {

        var savedOrder = orderServiceImpl.createNewOrder(dto, token);

        URI location = UriUtil.getUriLocation("orderId", savedOrder.orderId());

        return ResponseEntity.created(location).body(savedOrder);
    }

    @GetMapping("/ordersByUser")
    public ResponseEntity<OrdersDto> getUserOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                   @RequestHeader(name = "Cookie") String token) throws Exception {

        return ResponseEntity.ok(orderServiceImpl.getUserOrders(page, pageSize, token));
    }
}
