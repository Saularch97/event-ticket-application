package com.example.booking.controller;

import com.example.booking.controller.request.CreateOrderRequest;
import com.example.booking.controller.dto.OrderItemDto;
import com.example.booking.controller.dto.OrdersDto;
import com.example.booking.services.intefaces.OrderService;
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

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/order")
    public ResponseEntity<OrderItemDto> createNewOrder(
            @Valid
            @RequestBody CreateOrderRequest request
    )  {

        var savedOrder = orderService.createNewOrder(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedOrder.orderId())
                .toUri();

        return ResponseEntity.created(location).body(savedOrder);
    }

    @GetMapping("/orders")
    public ResponseEntity<OrdersDto> getUserOrders(@RequestParam(value = "page", defaultValue = "0") int page,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        var ordersDto = orderService.getUserOrders(page, pageSize);
        return ResponseEntity.ok(ordersDto);
    }

    @DeleteMapping("/order/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable("id") UUID orderId,
                                         @RequestHeader(name = "Cookie") String token) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}
