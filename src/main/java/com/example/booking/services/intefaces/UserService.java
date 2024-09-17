package com.example.booking.services.intefaces;

import com.example.booking.controller.dto.CreateUserDto;
import com.example.booking.controller.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto saveUser(CreateUserDto createUserDto);
    List<UserDto> listAllUsers();
}
