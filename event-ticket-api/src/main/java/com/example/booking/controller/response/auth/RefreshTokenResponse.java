package com.example.booking.controller.response.auth;

import org.springframework.http.ResponseCookie;

public record RefreshTokenResponse(ResponseCookie jwtCookie) {}
