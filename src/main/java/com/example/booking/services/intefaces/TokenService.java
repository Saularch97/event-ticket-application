package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.LoginRequest;
import com.example.booking.controller.dto.LoginResponse;

public interface TokenService {
    LoginResponse login(LoginRequest loginRequest);
}
