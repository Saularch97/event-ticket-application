package com.example.booking.entities.dto;


import java.util.List;

public record EventsDto(List<EventItemDto> events,
                         int page,
                         int pageSize,
                         int totalPages,
                         long totalElements) {
}
