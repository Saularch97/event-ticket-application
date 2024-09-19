package com.example.booking.controller.dto;

import java.util.UUID;

public record UserDto(UUID userId, String userName, String email) {
}
