package com.example.booking.services.intefaces;

import com.example.booking.controller.request.auth.LoginRequest;
import com.example.booking.controller.response.auth.LoginResponse;

public interface TokenService {
    LoginResponse login(LoginRequest loginRequest);
}
