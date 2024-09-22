package com.example.booking.controller.dto;

import java.util.Set;

import jakarta.validation.constraints.*;

public class SignupRequestDto {
    @NotBlank(message = "The user name is obrigatory")
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank(message = "The password is obrigatory")
    @Size(max = 50)
    @Email(message = "The email must be valid!")
    private String email;

    private Set<String> role;

    @NotBlank(message = "The user name is obrigatory")
    @Size(min = 6, max = 40)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRole() {
        return this.role;
    }

    public void setRole(Set<String> role) {
        this.role = role;
    }
}
