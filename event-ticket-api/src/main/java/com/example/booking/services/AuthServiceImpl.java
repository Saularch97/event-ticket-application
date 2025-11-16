package com.example.booking.services;

import java.util.*;
import java.util.stream.Collectors;

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
import com.example.booking.services.intefaces.AuthService;
import com.example.booking.services.intefaces.RoleService;
import com.example.booking.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           RoleService roleService,
                           PasswordEncoder encoder,
                           JwtUtils jwtUtils,
                           RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Login attempt for username={}", loginRequest.username());

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());
        ResponseCookie refreshCookie = jwtUtils.generateRefreshJwtCookie(refreshToken.getToken());

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        log.info("Login successful for userId={} username={}", userDetails.getId(), userDetails.getUsername());

        UserInfoResponse userInfo = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );

        return new AuthResponse(jwtCookie, refreshCookie, userInfo);
    }

    public UserDto registerUser(SignupRequest signUpRequest) {
        log.info("Register attempt with username={} email={}", signUpRequest.username(), signUpRequest.email());

        if (userRepository.existsByUserName(signUpRequest.username())) {
            log.warn("Registration failed: username '{}' already exists", signUpRequest.username());
            throw new UserNameAlreadyExistsException();
        }

        if (userRepository.existsByEmail(signUpRequest.email())) {
            log.warn("Registration failed: email '{}' already exists", signUpRequest.email());
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setUserName(signUpRequest.username());
        user.setEmail(signUpRequest.email());
        user.setPassword(encoder.encode(signUpRequest.password()));

        Set<String> strRoles = signUpRequest.role();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            log.info("No role specified, assigning ROLE_USER to username={}", signUpRequest.username());
            Role userRole = roleService.findRoleEntityByName(ERole.ROLE_USER);
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                log.info("Assigning role '{}' to username={}", role, signUpRequest.username());
                switch (ERole.valueOf(role)) {
                    case ERole.ROLE_ADMIN:
                        Role adminRole = roleService.findRoleEntityByName(ERole.ROLE_ADMIN);
                        roles.add(adminRole);
                        break;
                    case ERole.ROLE_MANAGER:
                        Role modRole = roleService.findRoleEntityByName(ERole.ROLE_MANAGER);
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleService.findRoleEntityByName(ERole.ROLE_USER);
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        log.info("User registered successfully with userId={} username={}", savedUser.getUserId(), savedUser.getUserName());

        return User.toUserDto(savedUser);
    }

    public CookieParDto logoutUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!"anonymousUser".equals(principal.toString())) {
            UUID userId = ((UserDetailsImpl) principal).getId();
            refreshTokenService.deleteByUserId(userId);

            log.info("User logged out with userId={}", userId);
        } else {
            log.warn("Logout called but no authenticated user found");
        }

        return new CookieParDto(jwtUtils.getCleanJwtCookie(), jwtUtils.getCleanJwtRefreshCookie());
    }

    public RefreshTokenResponse refreshToken(HttpServletRequest request) {
        String refreshToken = jwtUtils.getJwtRefreshFromCookies(request);

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("Refresh token missing in refresh token request");
            throw new RefreshTokenEmptyException();
        }

        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> {
                    log.warn("Refresh token '{}' not found or expired", refreshToken);
                    return new TokenRefreshException(refreshToken, "Refresh token is not in database!");
                });

        log.info("Refresh token verified successfully for userId={}", token.getUser().getUserId());

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(token.getUser());
        return new RefreshTokenResponse(jwtCookie);
    }
}
