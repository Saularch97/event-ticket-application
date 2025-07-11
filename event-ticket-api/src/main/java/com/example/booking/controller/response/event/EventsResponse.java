package com.example.booking.controller.response.event;

import com.example.booking.dto.EventSummaryDto;

import java.util.List;

public record EventsResponse(List<EventSummaryDto> events,
                             int page,
                             int pageSize,
                             int totalPages,
                             long totalElements) {
}
