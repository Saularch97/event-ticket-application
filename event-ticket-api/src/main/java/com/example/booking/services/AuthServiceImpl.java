package com.example.booking.services;


import java.util.*;
import java.util.stream.Collectors;

import com.example.booking.controller.dto.CookieParDto;
import com.example.booking.controller.dto.UserDto;
import com.example.booking.controller.request.LoginRequest;
import com.example.booking.controller.request.SignupRequest;
import com.example.booking.controller.response.AuthResponse;
import com.example.booking.controller.response.RefreshTokenResponse;
import com.example.booking.controller.response.UserInfoResponse;
import com.example.booking.domain.entities.RefreshToken;
import com.example.booking.domain.entities.Role;
import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.exception.TokenRefreshException;
import com.example.booking.repository.RoleRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.AuthService;
import com.example.booking.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {
    // TODO usar um role service e userService
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder encoder,
                           JwtUtils jwtUtils,
                           RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponse authenticateUser(LoginRequest loginRequest) {
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

        UserInfoResponse userInfo = new UserInfoResponse(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );

        return new AuthResponse(jwtCookie, refreshCookie, userInfo);
    }

    public UserDto registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUserName(signUpRequest.username())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }

        if (userRepository.existsByEmail(signUpRequest.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already in use");
        }

        User user = new User();
        user.setUserName(signUpRequest.username());
        user.setEmail(signUpRequest.email());
        user.setPassword(encoder.encode(signUpRequest.password()));

        Set<String> strRoles = signUpRequest.role();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);

        return User.toUserDto(userRepository.save(user));
    }

    public CookieParDto logoutUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!"anonymousUser".equals(principal.toString())) {
            UUID userId = ((UserDetailsImpl) principal).getId();
            refreshTokenService.deleteByUserId(userId);
        }

        return new CookieParDto(jwtUtils.getCleanJwtCookie(), jwtUtils.getCleanJwtRefreshCookie());
    }

    public RefreshTokenResponse refreshToken(HttpServletRequest request) {
        String refreshToken = jwtUtils.getJwtRefreshFromCookies(request);

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is empty");
        }

        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new TokenRefreshException(refreshToken, "Refresh token is not in database!"));

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(token.getUser());
        return new RefreshTokenResponse(jwtCookie);
    }
}
