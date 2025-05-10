package com.example.booking.controller.request;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(List<UUID> ticketIds) {
}
