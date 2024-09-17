package com.example.booking.entities.dto;

import java.util.List;
import java.util.UUID;

public record CreateNewOrderDto(List<UUID> ticketIds) {
}
