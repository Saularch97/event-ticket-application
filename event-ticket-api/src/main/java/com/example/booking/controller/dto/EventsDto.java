package com.example.booking.controller.dto;

import com.example.booking.dto.EventSummaryDto;

import java.util.List;

public record EventsDto(List<EventSummaryDto> events,
                         int page,
                         int pageSize,
                         int totalPages,
                         long totalElements) {
}
