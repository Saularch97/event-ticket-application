package com.example.booking.controller.dto;

import org.springframework.http.ResponseCookie;

public record CookieParDto(ResponseCookie jwt, ResponseCookie refresh) {
}
