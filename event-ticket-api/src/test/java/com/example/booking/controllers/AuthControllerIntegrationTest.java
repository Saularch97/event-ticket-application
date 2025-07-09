package com.example.booking.controllers;

import com.example.booking.controller.request.auth.LoginRequest;
import com.example.booking.controller.request.auth.SignupRequest;
import com.example.booking.domain.enums.ERole;
import com.example.booking.repositories.RoleRepository;
import com.example.booking.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    private final String testUsername = "testuser";
    private final String testPassword = "testpassword";

    @BeforeEach
    void setup() {

        SignupRequest request = new SignupRequest(testUsername, "testuser@example.com", Set.of(ERole.ROLE_USER.name()), testPassword);

        try {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test userid", e);
        }
    }

    @Test
    void shouldLoginAndReturnJwtCookie() throws Exception {
        LoginRequest login = new LoginRequest(testUsername, testPassword);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.username").value(testUsername));
    }

    @Test
    void shouldClearCookiesOnLogout() throws Exception {
        LoginRequest login = new LoginRequest(testUsername, testPassword);

        String authCookie = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        mockMvc.perform(post("/api/auth/signout")
                        .header(HttpHeaders.COOKIE, authCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                .andExpect(jsonPath("$.message").value("You've been signed out!"));
    }

    @Test
    void shouldRefreshTokenWhenValidRefreshCookieIsPresent() throws Exception {
        LoginRequest loginRequest = new LoginRequest(testUsername, testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshTokenCookie = loginResult.getResponse().getCookie("booking-jwt-refresh");
        assertNotNull(refreshTokenCookie, "The refresh cookie should not be null after the login");

        mockMvc.perform(post("/api/auth/refreshtoken")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("booking-test-jwt=")))
                .andExpect(jsonPath("$.message").value("Token is refreshed successfully!"));
    }
}
