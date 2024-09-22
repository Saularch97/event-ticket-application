package com.example.booking.controller;

import com.example.booking.controller.dto.CreateUserDto;
import com.example.booking.controller.dto.UserDto;

import com.example.booking.services.intefaces.UserService;
import com.example.booking.util.UriUtil;
import jakarta.transaction.Transactional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@Transactional
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<UserDto> newUser(@RequestBody CreateUserDto createUserDto) {

        var savedUser = userService.saveUser(createUserDto);

        URI location = UriUtil.getUriLocation("userId", savedUser.userId());

        return ResponseEntity.created(location).body(savedUser);
    }

    @GetMapping("/users")
//    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<List<UserDto>> listAllUsers() {
        return ResponseEntity.ok(userService.listAllUsers());
    }
}
