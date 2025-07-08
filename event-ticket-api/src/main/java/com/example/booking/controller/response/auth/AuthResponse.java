package com.example.booking.controller.response.auth;

import org.springframework.http.ResponseCookie;

public record AuthResponse(ResponseCookie jwtCookie, ResponseCookie jwtRefreshCookie, UserInfoResponse userInfo) {}

