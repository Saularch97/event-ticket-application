package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.CookieParDto;
import com.example.booking.controller.dto.UserDto;
import com.example.booking.controller.request.LoginRequest;
import com.example.booking.controller.request.SignupRequest;
import com.example.booking.controller.response.AuthResponse;
import com.example.booking.controller.response.RefreshTokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse authenticateUser(LoginRequest loginRequest);

    UserDto registerUser(SignupRequest signUpRequest);

    CookieParDto logoutUser();

    RefreshTokenResponse refreshToken(HttpServletRequest request);

}

