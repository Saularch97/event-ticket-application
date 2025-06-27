package com.example.booking.services.intefaces;

import com.example.booking.controller.request.CreateUserRequest;
import com.example.booking.controller.dto.UserDto;
import com.example.booking.domain.entities.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDto saveUser(CreateUserRequest createUserRequest);
    List<UserDto> listAllUsers();
    UserDto findByUserName(String username);
    User findUserEntityByUserName(String username);
    User findUserEntityById(UUID userId);
}
