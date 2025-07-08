package com.example.booking.dto;

import java.util.List;
import java.util.UUID;

public record UserDto(UUID userId, String userName, String email, List<RoleItemDto> scopes) {
}
