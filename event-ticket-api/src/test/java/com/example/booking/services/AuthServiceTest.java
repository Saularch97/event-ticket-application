package com.example.booking.services;

import com.example.booking.config.security.UserDetailsImpl;
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
import com.example.booking.exception.EmailAlreadyExistsException;
import com.example.booking.exception.RefreshTokenEmptyException;
import com.example.booking.exception.TokenRefreshException;
import com.example.booking.exception.UserNameAlreadyExistsException;
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
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.will;
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

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String TEST_REFRESH_TOKEN = "refreshToken";
    private static final String ROLE_USER_STR = "ROLE_USER";

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@example.com";

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String REFRESH_COOKIE_NAME = "refresh";
    private static final String EXAMPLE_JWT_TOKEN = "token";
    private static final String EXAMPLE_NEW_JWT_TOKEN = "newToken";
    private static final String EMPTY_COOKIE_VALUE = "";

    private static final String ANONYMOUS_USER_STR = "anonymousUser";

    private static final String VALID_REFRESH_TOKEN = "validRefreshToken";
    private static final String INVALID_REFRESH_TOKEN = "invalidToken";

    private static final String MSG_REFRESH_TOKEN_NOT_FOUND_FORMAT = "Failed for [%s]: Refresh token is not in database!";


    private LoginRequest loginRequest;
    private SignupRequest signupRequest;
    private User user;
    private Role adminRole;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest(TEST_USERNAME, TEST_PASSWORD);
        signupRequest = new SignupRequest(TEST_USERNAME, TEST_EMAIL, Set.of(ERole.ROLE_ADMIN.name()), TEST_PASSWORD);

        Role userRole = new Role();
        userRole.setName(ERole.ROLE_USER);

        adminRole = new Role();
        adminRole.setName(ERole.ROLE_ADMIN);

        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setPassword(ENCODED_PASSWORD);
        user.setRoles(Set.of(userRole));

        refreshToken = new RefreshToken();
        refreshToken.setToken(TEST_REFRESH_TOKEN);
        refreshToken.setUser(user);
    }

    @Test
    void authenticateUser_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        Authentication authentication = mock(Authentication.class);

        List<GrantedAuthority> roles = List.of(new SimpleGrantedAuthority(ROLE_USER_STR));
        UserDetailsImpl userDetails = spy(new UserDetailsImpl(
                user.getUserId(), user.getUserName(), user.getEmail(),
                TEST_PASSWORD, roles
        ));

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        ResponseCookie jwtCookie = ResponseCookie.from(JWT_COOKIE_NAME, EXAMPLE_JWT_TOKEN).build();
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, TEST_REFRESH_TOKEN).build();

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
        assertTrue(userInfo.getRoles().contains(ROLE_USER_STR));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenService).createRefreshToken(user.getUserId());
    }


    @Test
    void registerUser_ShouldReturnUserDto_WhenRegistrationIsSuccessful() {
        when(userRepository.existsByUserName(signupRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(signupRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(signupRequest.password())).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = authServiceImpl.registerUser(signupRequest);

        assertNotNull(result);
        assertEquals(user.getUserId(), result.userId());
        assertEquals(user.getUserName(), result.userName());
        assertEquals(user.getEmail(), result.email());

        verify(userRepository).existsByUserName(signupRequest.username());
        verify(userRepository).existsByEmail(signupRequest.email());
        verify(passwordEncoder).encode(signupRequest.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenUsernameIsTaken() {
        when(userRepository.existsByUserName(signupRequest.username())).thenReturn(true);

        assertThrows(UserNameAlreadyExistsException.class, () -> authServiceImpl.registerUser(signupRequest));

        verify(userRepository).existsByUserName(signupRequest.username());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailIsTaken() {
        when(userRepository.existsByUserName(signupRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(signupRequest.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> authServiceImpl.registerUser(signupRequest));

        verify(userRepository).existsByUserName(signupRequest.username());
        verify(userRepository).existsByEmail(signupRequest.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_ShouldAssignAdminRole_WhenAdminRoleIsRequested() {
        SignupRequest adminSignupRequest = new SignupRequest(ADMIN_USERNAME, ADMIN_EMAIL, Set.of(ERole.ROLE_ADMIN.name()), TEST_PASSWORD);

        when(userRepository.existsByUserName(adminSignupRequest.username())).thenReturn(false);
        when(userRepository.existsByEmail(adminSignupRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(adminSignupRequest.password())).thenReturn(ENCODED_PASSWORD);
        when(roleService.findRoleEntityByName(ERole.ROLE_ADMIN)).thenReturn(adminRole);

        User adminUser = new User();
        adminUser.setUserId(UUID.randomUUID());
        adminUser.setUserName(ADMIN_USERNAME);
        adminUser.setEmail(ADMIN_EMAIL);
        adminUser.setPassword(ENCODED_PASSWORD);
        adminUser.setRoles(Set.of(adminRole));

        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        UserDto result = authServiceImpl.registerUser(adminSignupRequest);

        assertEquals(User.toUserDto(adminUser), result);

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

        ResponseCookie cleanJwtCookie = ResponseCookie.from(JWT_COOKIE_NAME, EMPTY_COOKIE_VALUE).build();
        ResponseCookie cleanRefreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, EMPTY_COOKIE_VALUE).build();

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
        when(authentication.getPrincipal()).thenReturn(ANONYMOUS_USER_STR);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResponseCookie cleanJwtCookie = ResponseCookie.from(JWT_COOKIE_NAME, EMPTY_COOKIE_VALUE).build();
        ResponseCookie cleanRefreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, EMPTY_COOKIE_VALUE).build();

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
        when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn(VALID_REFRESH_TOKEN);

        when(refreshTokenService.findByToken(VALID_REFRESH_TOKEN))
                .thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);

        ResponseCookie newJwtCookie = ResponseCookie.from(JWT_COOKIE_NAME, EXAMPLE_NEW_JWT_TOKEN).build();
        when(jwtUtils.generateJwtCookie(refreshToken.getUser())).thenReturn(newJwtCookie);

        RefreshTokenResponse result = authServiceImpl.refreshToken(request);

        assertNotNull(result);
        assertEquals(newJwtCookie, result.jwtCookie());

        verify(jwtUtils).getJwtRefreshFromCookies(request);
        verify(refreshTokenService).findByToken(VALID_REFRESH_TOKEN);
        verify(refreshTokenService).verifyExpiration(refreshToken);
        verify(jwtUtils).generateJwtCookie(refreshToken.getUser());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenRefreshTokenIsEmpty() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn(EMPTY_COOKIE_VALUE);

        assertThrows(RefreshTokenEmptyException.class,
                () -> authServiceImpl.refreshToken(request));

        verify(jwtUtils).getJwtRefreshFromCookies(request);
        verify(refreshTokenService, never()).findByToken(any());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenRefreshTokenIsNotFound() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtUtils.getJwtRefreshFromCookies(request)).thenReturn(INVALID_REFRESH_TOKEN);
        when(refreshTokenService.findByToken(INVALID_REFRESH_TOKEN)).thenReturn(Optional.empty());

        TokenRefreshException exception = assertThrows(TokenRefreshException.class,
                () -> authServiceImpl.refreshToken(request));

        assertEquals(String.format(MSG_REFRESH_TOKEN_NOT_FOUND_FORMAT, INVALID_REFRESH_TOKEN), exception.getMessage());

        verify(jwtUtils).getJwtRefreshFromCookies(request);
        verify(refreshTokenService).findByToken(INVALID_REFRESH_TOKEN);
        verify(refreshTokenService, never()).verifyExpiration(any());
    }
}
