package com.example.booking.services.intefaces;

import com.example.booking.controller.request.CreateUserRequest;
import com.example.booking.controller.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto saveUser(CreateUserRequest createUserRequest);
    List<UserDto> listAllUsers();
}
