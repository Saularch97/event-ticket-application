package com.example.booking.services;

import com.example.booking.controller.request.auth.CreateUserRequest;
import com.example.booking.dto.UserDto;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private CreateUserRequest createUserRequest;
    // TODO colocar strings em constantes para serem usadas
    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setRoles(Set.of(new Role(ERole.ROLE_USER)));

        createUserRequest = new CreateUserRequest(
                "testuser",
                "test@example.com",
                "password"
        );
    }

    @Test
    void saveUser_ShouldReturnUserDto_WhenRequestIsValid() {
        when(repository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.saveUser(createUserRequest);

        assertNotNull(result);
        assertEquals(user.getUserName(), result.userName());
        assertEquals(user.getEmail(), result.email());
        verify(repository).save(any(User.class));
    }

    @Test
    void saveUser_ShouldMapAllFieldsCorrectly() {
        when(repository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.saveUser(createUserRequest);

        assertEquals(createUserRequest.username(), result.userName());
        assertEquals(createUserRequest.email(), result.email());
        assertEquals(1, result.scopes().size());
        assertEquals("ROLE_USER", result.scopes().get(0).name());
    }

    @Test
    void listAllUsers_ShouldReturnListOfUserDtos_WhenUsersExist() {
        User user2 = new User();
        user2.setUserName("anotheruser");
        user2.setEmail("another@example.com");
        user2.setRoles(Set.of(new Role(ERole.ROLE_ADMIN)));

        when(repository.findAll()).thenReturn(List.of(user, user2));

        List<UserDto> result = userService.listAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(user.getUserName(), result.get(0).userName());
        assertEquals(user2.getUserName(), result.get(1).userName());
        verify(repository).findAll();
    }

    @Test
    void listAllUsers_ShouldReturnEmptyList_WhenNoUsersExist() {
        when(repository.findAll()).thenReturn(List.of());

        List<UserDto> result = userService.listAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }

    @Test
    void findByUserName_ShouldReturnUserDto_WhenUserExists() {
        when(repository.findByUserName("testuser")).thenReturn(Optional.of(user));

        UserDto result = userService.findByUserName("testuser");

        assertNotNull(result);
        assertEquals(user.getUserName(), result.userName());
        assertEquals(user.getEmail(), result.email());
        assertEquals(1, result.scopes().size());
        verify(repository).findByUserName("testuser");
    }

    @Test
    void findByUserName_ShouldThrowException_WhenUserNotFound() {
        when(repository.findByUserName("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.findByUserName("nonexistent")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found!", exception.getReason());
        verify(repository).findByUserName("nonexistent");
    }

    @Test
    void findEntityByUserName_ShouldReturnUserUserEntity_WhenUserExists() {
        when(repository.findByUserName("testuser")).thenReturn(Optional.of(user));

        User result = userService.findUserEntityByUserName("testuser");

        assertNotNull(result);
        assertEquals(user.getUserName(), result.getUserName());
        assertEquals(user.getEmail(), result.getEmail());
        verify(repository).findByUserName("testuser");
    }

    @Test
    void findUserEntityByUserName_ShouldThrowException_WhenUserNotFound() {
        when(repository.findByUserName("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.findUserEntityByUserName("nonexistent")
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found!", exception.getReason());
        verify(repository).findByUserName("nonexistent");
    }

    @Test
    void saveUser_ShouldSetCorrectProperties_WhenCreatingNewUser() {
        CreateUserRequest request = new CreateUserRequest(
                "newuser",
                "new@example.com",
                "newpassword"
        );

        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setUserId(UUID.randomUUID());
            savedUser.setRoles(Set.of(new Role(ERole.ROLE_ADMIN)));
            return savedUser;
        });

        UserDto result = userService.saveUser(request);

        verify(repository).save(argThat(user ->
                user.getUserName().equals("newuser") &&
                        user.getEmail().equals("new@example.com") &&
                        user.getPassword().equals("newpassword") &&
                        user.getRoles().iterator().next().getName() == ERole.ROLE_ADMIN
        ));
        assertEquals("newuser", result.userName());
        assertEquals("new@example.com", result.email());
    }

    @Test
    void findByUserName_ShouldIncludeRolesInResponse() {
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        user.setRoles(Set.of(adminRole));

        when(repository.findByUserName("testuser")).thenReturn(Optional.of(user));

        UserDto result = userService.findByUserName("testuser");

        assertEquals(1, result.scopes().size());
        assertEquals("ROLE_ADMIN", result.scopes().getFirst().name());
    }
}
