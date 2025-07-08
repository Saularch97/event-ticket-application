package com.example.booking.controller.request.auth;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "Provide an username") String username,
        @NotBlank(message = "Provide an email")String email,
        @NotBlank(message = "Provide an password")String password
) {

}
