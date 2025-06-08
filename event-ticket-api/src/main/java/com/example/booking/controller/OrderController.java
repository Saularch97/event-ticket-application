package com.example.booking.controller;

import com.example.booking.controller.request.CreateOrderRequest;
import com.example.booking.controller.dto.OrderItemDto;
import com.example.booking.controller.dto.OrdersDto;
import com.example.booking.services.intefaces.OrderService;
import com.example.booking.util.JwtUtils;
import com.example.booking.util.UriUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtils jwtUtils;

    public OrderController(OrderService orderService, JwtUtils jwtUtils) {
        this.orderService = orderService;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    @PostMapping("/order")
    public ResponseEntity<OrderItemDto> createNewOrder(
            @Valid
            @RequestBody CreateOrderRequest dto,
            @RequestHeader(name = "Cookie") String token
    )  {

        var savedOrder = orderService.createNewOrder(dto, token);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedOrder.orderId())
                .toUri();

        return ResponseEntity.created(location).body(savedOrder);
    }

    @GetMapping("/orders")
    public ResponseEntity<OrdersDto> getUserOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                   @RequestHeader(name = "Cookie") String token) {

        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);
        var ordersDto = orderService.getUserOrders(page, pageSize, userName);
        return ResponseEntity.ok(ordersDto);
    }

    @DeleteMapping("/order/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable("id") UUID orderId,
                                         @RequestHeader(name = "Cookie") String token) {
        String userName = jwtUtils.getUserNameFromJwtToken(token.split(";")[0].split("=")[1]);
        orderService.deleteOrder(orderId, userName);
        return ResponseEntity.noContent().build();
    }
}
