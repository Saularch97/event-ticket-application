package com.example.booking.controller.dto;

import java.util.List;
import java.util.UUID;

public record CreateNewOrderDto(List<UUID> ticketIds) {
}
