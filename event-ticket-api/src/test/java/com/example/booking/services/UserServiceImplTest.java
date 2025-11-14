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

    private static final String TEST_USER_USERNAME = "testuser";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_USER_PASSWORD = "password";
    private static final String NEW_USER_USERNAME = "newuser";
    private static final String NEW_USER_EMAIL = "new@example.com";
    private static final String NEW_USER_PASSWORD = "newpassword";
    private static final String USER2_USERNAME = "anotheruser";
    private static final String USER2_EMAIL = "another@example.com";
    private static final String NONEXISTENT_USERNAME = "nonexistent";
    private static final String MSG_USER_NOT_FOUND = "User not found!";
    private static final String ROLE_USER_STR = "ROLE_USER";
    private static final String ROLE_ADMIN_STR = "ROLE_ADMIN";
    private static final int EXPECTED_SIZE_1 = 1;
    private static final int EXPECTED_SIZE_2 = 2;

    private User user;
    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName(TEST_USER_USERNAME);
        user.setEmail(TEST_USER_EMAIL);
        user.setPassword(TEST_USER_PASSWORD);
        user.setRoles(Set.of(new Role(ERole.ROLE_USER)));

        createUserRequest = new CreateUserRequest(
                TEST_USER_USERNAME,
                TEST_USER_EMAIL,
                TEST_USER_PASSWORD
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
        assertEquals(EXPECTED_SIZE_1, result.scopes().size());
        assertEquals(ROLE_USER_STR, result.scopes().get(0).name());
    }

    @Test
    void listAllUsers_ShouldReturnListOfUserDtos_WhenUsersExist() {
        User user2 = new User();
        user2.setUserName(USER2_USERNAME);
        user2.setEmail(USER2_EMAIL);
        user2.setRoles(Set.of(new Role(ERole.ROLE_ADMIN)));

        when(repository.findAll()).thenReturn(List.of(user, user2));

        List<UserDto> result = userService.listAllUsers();

        assertNotNull(result);
        assertEquals(EXPECTED_SIZE_2, result.size());
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
        when(repository.findByUserName(TEST_USER_USERNAME)).thenReturn(Optional.of(user));

        UserDto result = userService.findByUserName(TEST_USER_USERNAME);

        assertNotNull(result);
        assertEquals(user.getUserName(), result.userName());
        assertEquals(user.getEmail(), result.email());
        assertEquals(EXPECTED_SIZE_1, result.scopes().size());
        verify(repository).findByUserName(TEST_USER_USERNAME);
    }

    @Test
    void findByUserName_ShouldThrowException_WhenUserNotFound() {
        when(repository.findByUserName(NONEXISTENT_USERNAME)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.findByUserName(NONEXISTENT_USERNAME)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(MSG_USER_NOT_FOUND, exception.getReason());
        verify(repository).findByUserName(NONEXISTENT_USERNAME);
    }

    @Test
    void findEntityByUserName_ShouldReturnUserUserEntity_WhenUserExists() {
        when(repository.findByUserName(TEST_USER_USERNAME)).thenReturn(Optional.of(user));

        User result = userService.findUserEntityByUserName(TEST_USER_USERNAME);

        assertNotNull(result);
        assertEquals(user.getUserName(), result.getUserName());
        assertEquals(user.getEmail(), result.getEmail());
        verify(repository).findByUserName(TEST_USER_USERNAME);
    }

    @Test
    void findUserEntityByUserName_ShouldThrowException_WhenUserNotFound() {
        when(repository.findByUserName(NONEXISTENT_USERNAME)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.findUserEntityByUserName(NONEXISTENT_USERNAME)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(MSG_USER_NOT_FOUND, exception.getReason());
        verify(repository).findByUserName(NONEXISTENT_USERNAME);
    }

    @Test
    void saveUser_ShouldSetCorrectProperties_WhenCreatingNewUser() {
        CreateUserRequest request = new CreateUserRequest(
                NEW_USER_USERNAME,
                NEW_USER_EMAIL,
                NEW_USER_PASSWORD
        );

        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setUserId(UUID.randomUUID());
            savedUser.setRoles(Set.of(new Role(ERole.ROLE_ADMIN)));
            return savedUser;
        });

        UserDto result = userService.saveUser(request);

        verify(repository).save(argThat(user ->
                user.getUserName().equals(NEW_USER_USERNAME) &&
                        user.getEmail().equals(NEW_USER_EMAIL) &&
                        user.getPassword().equals(NEW_USER_PASSWORD) &&
                        user.getRoles().iterator().next().getName() == ERole.ROLE_ADMIN
        ));
        assertEquals(NEW_USER_USERNAME, result.userName());
        assertEquals(NEW_USER_EMAIL, result.email());
    }

    @Test
    void findByUserName_ShouldIncludeRolesInResponse() {
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        user.setRoles(Set.of(adminRole));

        when(repository.findByUserName(TEST_USER_USERNAME)).thenReturn(Optional.of(user));

        UserDto result = userService.findByUserName(TEST_USER_USERNAME);

        assertEquals(EXPECTED_SIZE_1, result.scopes().size());
        assertEquals(ROLE_ADMIN_STR, result.scopes().getFirst().name());
    }
}