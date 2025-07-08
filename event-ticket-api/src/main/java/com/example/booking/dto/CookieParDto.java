package com.example.booking.dto;

import org.springframework.http.ResponseCookie;

public record CookieParDto(ResponseCookie jwt, ResponseCookie refresh) {
}
