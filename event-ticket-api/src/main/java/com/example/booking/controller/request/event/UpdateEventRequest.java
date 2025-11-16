package com.example.booking.controller.request.event;

import com.example.booking.controller.request.ticket.CreateTicketCategoryRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateEventRequest(
        @NotBlank(message = "Event needs to have a name")
        String eventName,

        @NotNull(message = "Event needs to have a date and time")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime eventDateTime,

        @NotBlank(message = "Event needs to have a location")
        String eventLocation
) { }
