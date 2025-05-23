package com.example.booking.controller.response;

import com.example.booking.controller.dto.EventItemDto;

import java.util.List;

public record EventsResponse(List<EventItemDto> events,
                             int page,
                             int pageSize,
                             int totalPages,
                             long totalElements) {
}
