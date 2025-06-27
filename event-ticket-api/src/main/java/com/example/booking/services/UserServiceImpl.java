package com.example.booking.services;

import com.example.booking.controller.request.CreateUserRequest;
import com.example.booking.controller.dto.UserDto;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
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

        var res = repository.save(user);

        return User.toUserDto(res);
    }

    @Override
    public List<UserDto> listAllUsers() {
        return repository.findAll().stream().map(User::toUserDto).collect(Collectors.toList());
    }

    @Override
    public UserDto findByUserName(String username) {
        var user = repository.findByUserName(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));
        
        return new UserDto(user.getUserId(), user.getUserName(), user.getEmail(), user.getRoles().stream().map(Role::toRoleItemDto).toList());
    }

    @Override
    public User findUserEntityByUserName(String username) {
        return repository.findByUserName(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));
    }

    @Override
    public User findUserEntityById(UUID userId) {
        return repository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));
    }
}
