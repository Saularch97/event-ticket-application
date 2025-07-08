package com.example.booking.services.intefaces;

import com.example.booking.dto.CookieParDto;
import com.example.booking.dto.UserDto;
import com.example.booking.controller.request.auth.LoginRequest;
import com.example.booking.controller.request.auth.SignupRequest;
import com.example.booking.controller.response.auth.AuthResponse;
import com.example.booking.controller.response.auth.RefreshTokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse authenticateUser(LoginRequest loginRequest);

    UserDto registerUser(SignupRequest signUpRequest);

    CookieParDto logoutUser();

    RefreshTokenResponse refreshToken(HttpServletRequest request);

}

