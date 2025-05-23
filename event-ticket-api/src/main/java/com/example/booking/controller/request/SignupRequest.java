package com.example.booking.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SignupRequest(

        @NotBlank(message = "The user name is obligatory")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String username,

        @NotBlank(message = "The email is obligatory")
        @Size(max = 50, message = "Email must be at most 50 characters")
        @Email(message = "The email must be valid")
        String email,

        @NotEmpty(message = "At least one role must be specified")
        Set<String> role,

        @NotBlank(message = "The password is obligatory")
        @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
        String password

) {}
