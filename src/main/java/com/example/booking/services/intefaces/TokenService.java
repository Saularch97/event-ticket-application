package com.example.booking.services.intefaces;

import com.example.booking.controller.request.LoginRequest;
import com.example.booking.controller.response.LoginResponse;

public interface TokenService {
    LoginResponse login(LoginRequest loginRequest);
}
