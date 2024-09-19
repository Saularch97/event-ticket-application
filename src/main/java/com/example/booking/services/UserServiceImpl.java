package com.example.booking.services;

import com.example.booking.controller.dto.CreateUserDto;
import com.example.booking.controller.dto.UserDto;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.repository.RoleRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.UserService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository repository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto saveUser(CreateUserDto createUserDto) {
        var basicRole = roleRepository.findByName(Role.Values.BASIC.name().toLowerCase());

        var userFromDb = repository.findByUserName(createUserDto.username());

        if (userFromDb.isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }

        var user = new User();
        user.setUserName(createUserDto.username());
        user.setPassword(passwordEncoder.encode(createUserDto.password()));
        user.setEmail(createUserDto.email());
        user.setRoles(Set.of(basicRole));

        return repository.save(user).toUserDto();
    }

    public List<UserDto> listAllUsers() {
        var users = repository.findAll();

        return users.stream().map(User::toUserDto).collect(Collectors.toList());
    }
}
