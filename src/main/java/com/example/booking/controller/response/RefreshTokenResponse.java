package com.example.booking.controller.response;

import org.springframework.http.ResponseCookie;

public record RefreshTokenResponse(ResponseCookie jwtCookie) {}
