package com.example.booking.dto;

import java.util.List;

public record EventsDto(List<EventSummaryDto> events,
                         int page,
                         int pageSize,
                         int totalPages,
                         long totalElements) {
}
