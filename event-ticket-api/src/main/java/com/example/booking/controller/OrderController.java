package com.example.booking.controller;

import com.example.booking.controller.request.order.CreateOrderRequest;
import com.example.booking.controller.response.order.CreateOrderResponse;
import com.example.booking.controller.response.order.OrdersResponse;
import com.example.booking.dto.OrderItemDto;
import com.example.booking.services.intefaces.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Orders", description = "Endpoints for creating and managing ticket orders.")
@RestController
@RequestMapping("/api/orders") // Changed to /api/orders for REST consistency
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(
            summary = "Create a new order",
            description = "Creates a new order from a list of ticket IDs. The userid is identified by the authentication token.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Order created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CreateOrderResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request, e.g., no ticket IDs provided"),
                    @ApiResponse(responseCode = "404", description = "One or more tickets not found"),
                    @ApiResponse(responseCode = "409", description = "One of the tickets already have an order")
            }
    )
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createNewOrder(
            @Valid @RequestBody CreateOrderRequest request
    )  {
        OrderItemDto orderItemDto = orderService.createNewOrder(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(orderItemDto.orderId())
                .toUri();
        return ResponseEntity.created(location).body(new CreateOrderResponse(orderItemDto.orderId(), orderItemDto.checkoutUrl()));
    }

    @Operation(
            summary = "Get user's orders",
            description = "Retrieves a paginated list of orders for the authenticated user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Orders retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = OrdersResponse.class)
                            )
                    )
            }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<OrdersResponse> getUserOrders(
            @Parameter(description = "User id") @PathVariable UUID userId,
            @Parameter(description = "Page number to retrieve") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of orders per page") @RequestParam(defaultValue = "10") int pageSize
    ) {
        var ordersDto = orderService.getOrdersByUserId(userId,page, pageSize);
        return ResponseEntity.ok(ordersDto);
    }

    @Operation(
            summary = "Delete an order",
            description = "Deletes an order by its ID. The userid must be the owner of the order.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Order not found"),
                    @ApiResponse(responseCode = "403", description = "User is not authorized to delete this order")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "ID of the order to be deleted", required = true)
            @PathVariable("id") UUID orderId
    ) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }
}