package com.example.booking.controller;

import com.example.booking.controller.request.auth.LoginRequest;
import com.example.booking.controller.response.auth.AuthMessageResponse;
import com.example.booking.controller.request.auth.SignupRequest;
import com.example.booking.controller.response.auth.UserInfoResponse;
import com.example.booking.services.intefaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Endpoints for userid registration, sign-in, and token management.")
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Authenticate User",
            description = "Authenticates a userid with username and password. On success, it returns userid info in the body and sets JWT and refresh tokens in secure, HttpOnly cookies.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication successful",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserInfoResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AuthMessageResponse.class)))
            }
    )
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        var result = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.jwtCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.jwtRefreshCookie().toString())
                .body(result.userInfo());
    }

    @Operation(
            summary = "Register New User",
            description = "Registers a new userid in the system.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User registered successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AuthMessageResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Username or email already in use",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AuthMessageResponse.class)))
            }
    )
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        var result = authService.registerUser(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(
            summary = "Sign Out User",
            description = "Logs out the userid by clearing the authentication cookies.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Sign-out successful",
                            content = @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = AuthMessageResponse.class
                                    )
                            )
                    )
            }
    )
    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser() {
        var cookies = authService.logoutUser();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.jwt().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh().toString())
                .body(new AuthMessageResponse("You've been signed out!"));
    }

    @Operation(
            summary = "Refresh Access Token",
            description = "Generates a new access token (JWT) using a valid refresh token provided in a cookie.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthMessageResponse.class
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthMessageResponse.class)
                            )
                    )
            }
    )
    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        var response = authService.refreshToken(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.jwtCookie().toString())
                .body(new AuthMessageResponse("Token is refreshed successfully!"));
    }
}