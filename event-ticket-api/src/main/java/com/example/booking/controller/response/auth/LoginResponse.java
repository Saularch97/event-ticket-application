package com.example.booking.controller.response.auth;

public record LoginResponse(String accessToken, Long expiresIn) {
}
