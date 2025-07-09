package com.example.booking.services;

import com.example.booking.dto.CookieParDto;
import com.example.booking.dto.UserDto;
import com.example.booking.controller.request.auth.LoginRequest;
import com.example.booking.controller.request.auth.SignupRequest;
import com.example.booking.controller.response.auth.AuthResponse;
import com.example.booking.controller.response.auth.RefreshTokenResponse;
import com.example.booking.controller.response.auth.UserInfoResponse;
import com.example.booking.domain.entities.RefreshToken;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.exception.TokenRefreshException;
import com.example.booking.repositories.UserRepository;
import com.example.booking.services.intefaces.RoleService;
import com.example.booking.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private User user;
    private Role userRole;
    private Role adminRole;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("testuser", "password");
        signupRequest = new SignupRequest("testuser", "test@example.com", Set.of(ERole.ROLE_ADMIN.name()), "password");
        
        userRole = new Role();
        userRole.setName(ERole.ROLE_USER);
        
        adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRoles(Set.of(userRole));
        
        refreshToken = new RefreshToken();
        refreshToken.setToken("refreshToken");
        refreshToken.setUser(user);
    }

    @Test
    void authenticateUser_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        Authentication authentication = mock(Authentication.class);

        List<GrantedAuthority> roles = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetailsImpl userDetails = spy(new UserDetailsImpl(
                user.getUserId(), user.getUserName(), user.getEmail(),
                "password", roles
        ));

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "token").build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh", "refreshToken").build();

        when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
        when(jwtUtils.generateRefreshJwtCookie(any())).thenReturn(refreshCookie);
        when(refreshTokenService.createRefreshToken(user.getUserId())).thenReturn(refreshToken);

        AuthResponse response = authServiceImpl.authenticateUser(loginRequest);

        assertNotNull(response);
        assertEquals(jwtCookie, response.jwtCookie());
        assertEquals(refreshCookie, response.jwtRefreshCookie());

        UserInfoResponse userInfo = response.userInfo();
        assertEquals(user.getUserId(), userInfo.getId());
        assertEquals(user.getUserName(), userInfo.getUsername());
        assertEquals(user.getEmail(), userInfo.getEmail());
        assertEquals(1, userInfo.getRoles().size());
        assertTrue(userInfo.getRoles().contains("ROLE_USER"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).createRefreshToken(user.getUserId());
    }


    @Test
    void registerUser_ShouldReturnUserDto_WhenRegistrationIsSuccessful() {
        when(userRepository.existsByUserName(signupRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(signupRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(signupRequest.password())).thenReturn("encodedPassword");
        when(roleService.findRoleEntityByName(ERole.ROLE_USER)).thenReturn(userRole);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = authServiceImpl.registerUser(signupRequest);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.userId());
        assertEquals(user.getUserName(), result.userName());
        assertEquals(user.getEmail(), result.email());
        
        verify(userRepository).existsByUserName(signupRequest.username());
        verify(userRepository).existsByEmail(signupRequest.email());
        verify(passwordEncoder).encode(signupRequest.password());
        verify(roleService).findRoleEntityByName(ERole.ROLE_USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenUsernameIsTaken() {
        when(userRepository.existsByUserName(signupRequest.username())).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> authServiceImpl.registerUser(signupRequest));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Username is already taken", exception.getReason());
        
        verify(userRepository).existsByUserName(signupRequest.username());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailIsTaken() {
        when(userRepository.existsByUserName(signupRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(signupRequest.email())).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> authServiceImpl.registerUser(signupRequest));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Email is already in use", exception.getReason());
        
        verify(userRepository).existsByUserName(signupRequest.username());
        verify(userRepository).existsByEmail(signupRequest.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_ShouldAssignAdminRole_WhenAdminRoleIsRequested() {
        SignupRequest adminSignupRequest = new SignupRequest("admin", "admin@example.com", Set.of("admin"), "password");

        when(userRepository.existsByUserName(adminSignupRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(adminSignupRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(adminSignupRequest.password())).thenReturn("encodedPassword");
        when(roleService.findRoleEntityByName(ERole.ROLE_ADMIN)).thenReturn(adminRole);

        User adminUser = new User();
        adminUser.setUserId(UUID.randomUUID());
        adminUser.setUserName("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("encodedPassword");
        adminUser.setRoles(Set.of(adminRole));

        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        UserDto result = authServiceImpl.registerUser(adminSignupRequest);

        assertNotNull(result);
        assertEquals(adminUser.getUserId(), result.userId());
        assertEquals(adminUser.getUserName(), result.userName());

        verify(roleService).findRoleEntityByName(ERole.ROLE_ADMIN);
        verify(roleService, never()).findRoleEntityByName(ERole.ROLE_USER);
    }

    @Test
    void logoutUser_ShouldReturnCleanCookies_WhenUserIsAuthenticated() {
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(user.getUserId());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        ResponseCookie cleanJwtCookie = ResponseCookie.from("jwt", "").build();
        ResponseCookie cleanRefreshCookie = ResponseCookie.from("refresh", "").build();
        
        when(jwtUtils.getCleanJwtCookie()).thenReturn(cleanJwtCookie);
        when(jwtUtils.getCleanJwtRefreshCookie()).thenReturn(cleanRefreshCookie);

        CookieParDto result = authServiceImpl.logoutUser();

        assertNotNull(result);
        assertEquals(cleanJwtCookie, result.jwt());
        assertEquals(cleanRefreshCookie, result.refresh());
        
        verify(refreshTokenService).deleteByUserId(user.getUserId());
        verify(jwtUtils).getCleanJwtCookie();
        verify(jwtUtils).getCleanJwtRefreshCookie();
    }

    @Test
    void logoutUser_ShouldReturnCleanCookies_WhenUserIsAnonymous() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        ResponseCookie cleanJwtCookie = ResponseCookie.from("jwt", "").build();
        ResponseCookie cleanRefreshCookie = ResponseCookie.from("refresh", "").build();
        
        when(jwtUtils.getCleanJwtCookie()).thenReturn(cleanJwtCookie);
        when(jwtUtils.getCleanJwtRefreshCookie()).thenReturn(cleanRefreshCookie);

        CookieParDto result = authServiceImpl.logoutUser();

        assertNotNull(result);
        assertEquals(cleanJwtCookie, result.jwt());
        assertEquals(cleanRefreshCookie, result.refresh());
        
        verify(refreshTokenService, never()).deleteByUserId(any());
        verify(jwtUtils).getCleanJwtCookie();
        verify(jwtUtils).getCleanJwtRefreshCookie();
    }

    @Test
    void refreshToken_ShouldReturnNewJwtCookie_WhenRefreshTokenIsValid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn("validRefreshToken");
        
        when(refreshTokenService.findByToken("validRefreshToken"))
            .thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        
        ResponseCookie newJwtCookie = ResponseCookie.from("jwt", "newToken").build();
        when(jwtUtils.generateJwtCookie(refreshToken.getUser())).thenReturn(newJwtCookie);

        RefreshTokenResponse result = authServiceImpl.refreshToken(request);

        assertNotNull(result);
        assertEquals(newJwtCookie, result.jwtCookie());
        
        verify(jwtUtils).getJwtRefreshFromCookies(request);
        verify(refreshTokenService).findByToken("validRefreshToken");
        verify(refreshTokenService).verifyExpiration(refreshToken);
        verify(jwtUtils).generateJwtCookie(refreshToken.getUser());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenRefreshTokenIsEmpty() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn("");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
            () -> authServiceImpl.refreshToken(request));
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Refresh token is empty", exception.getReason());
        
        verify(jwtUtils).getJwtRefreshFromCookies(request);
        verify(refreshTokenService, never()).findByToken(any());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenRefreshTokenIsNotFound() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn("invalidToken");
        when(refreshTokenService.findByToken("invalidToken")).thenReturn(Optional.empty());

        TokenRefreshException exception = assertThrows(TokenRefreshException.class,
            () -> authServiceImpl.refreshToken(request));

        assertEquals("Failed for [invalidToken]: Refresh token is not in database!", exception.getMessage());

        verify(jwtUtils).getJwtRefreshFromCookies(request);
        verify(refreshTokenService).findByToken("invalidToken");
        verify(refreshTokenService, never()).verifyExpiration(any());
    }
}