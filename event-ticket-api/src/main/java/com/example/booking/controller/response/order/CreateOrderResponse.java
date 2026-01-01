package com.example.booking.controller.response.order;

import java.util.UUID;

public record CreateOrderResponse(UUID orderId, String checkoutUrl) {
}
