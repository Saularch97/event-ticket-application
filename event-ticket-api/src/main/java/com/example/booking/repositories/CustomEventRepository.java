package com.example.booking.repositories;

import com.example.booking.dto.EventSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface CustomEventRepository {
    Page<EventSummaryDto> findByCriteria(String name, String location, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}
