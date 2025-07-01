package com.example.booking.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventSummaryDto(UUID eventId, String name, String location, Integer availableTickets, LocalDateTime eventDate) {}

