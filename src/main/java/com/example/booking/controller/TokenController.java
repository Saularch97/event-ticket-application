//package com.example.booking.controller;
//
//import com.example.booking.controller.dto.LoginRequest;
//import com.example.booking.controller.dto.LoginResponse;
//import com.example.booking.services.TokenServiceImpl;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class TokenController {
//
//    private final TokenServiceImpl tokenServiceImpl;
//
//    public TokenController(TokenServiceImpl tokenServiceImpl) {
//        this.tokenServiceImpl = tokenServiceImpl;
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
//        return ResponseEntity.ok(tokenServiceImpl.login(loginRequest));
//    }
//}
