package com.example.booking.controller;

import com.example.booking.controller.request.LoginRequest;
import com.example.booking.controller.response.AuthMessageResponse;
import com.example.booking.controller.request.SignupRequest;
import com.example.booking.services.intefaces.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        var result = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.jwtCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.jwtRefreshCookie().toString())
                .body(result.userInfo());
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        var result = authService.registerUser(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        var cookies = authService.logoutUser();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.jwt().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh().toString())
                .body(new AuthMessageResponse("You've been signed out!"));
    }

    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        var response = authService.refreshToken(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.jwtCookie().toString())
                .body(new AuthMessageResponse("Token is refreshed successfully!"));
    }
}
