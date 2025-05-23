package com.example.booking.services;

import com.example.booking.controller.request.CreateUserRequest;
import com.example.booking.controller.dto.UserDto;
import com.example.booking.domain.entities.User;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final  UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDto saveUser(CreateUserRequest createUserRequest) {

        var user = new User();
        user.setUserName(createUserRequest.username());
        user.setEmail(createUserRequest.email());
        user.setPassword(createUserRequest.password());

        return User.toUserDto(user);
    }

    @Override
    public List<UserDto> listAllUsers() {
        return repository.findAll().stream().map(User::toUserDto).collect(Collectors.toList());
    }
}
