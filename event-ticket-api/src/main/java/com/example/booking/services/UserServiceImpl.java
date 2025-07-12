package com.example.booking.services;

import com.example.booking.controller.request.auth.CreateUserRequest;
import com.example.booking.dto.UserDto;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.repositories.UserRepository;
import com.example.booking.services.intefaces.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository repository;

    public UserServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDto saveUser(CreateUserRequest createUserRequest) {
        log.info("Creating new user with username={}", createUserRequest.username());

        var user = new User();
        user.setUserName(createUserRequest.username());
        user.setEmail(createUserRequest.email());
        user.setPassword(createUserRequest.password());

        var res = repository.save(user);

        log.info("User created successfully with id={}", res.getUserId());

        return User.toUserDto(res);
    }

    @Override
    public List<UserDto> listAllUsers() {
        log.info("Listing all users");
        return repository.findAll().stream()
                .peek(u -> log.debug("User found: id={}, username={}", u.getUserId(), u.getUserName()))
                .map(User::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto findByUserName(String username) {
        log.info("Searching for user by username={}", username);
        var user = repository.findByUserName(username).orElseThrow(() -> {
            log.warn("User not found with username={}", username);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
        });

        log.info("User found: id={}, username={}", user.getUserId(), user.getUserName());

        return new UserDto(user.getUserId(), user.getUserName(), user.getEmail(),
                user.getRoles().stream().map(Role::toRoleItemDto).toList());
    }

    @Override
    public User findUserEntityByUserName(String username) {
        log.debug("Finding user entity by username={}", username);

        return repository.findByUserName(username).orElseThrow(() -> {
            log.warn("User entity not found with username={}", username);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
        });
    }

    @Override
    public User findUserEntityById(UUID userId) {
        log.debug("Finding user entity by id={}", userId);

        return repository.findById(userId).orElseThrow(() -> {
            log.warn("User entity not found with id={}", userId);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!");
        });
    }
}
