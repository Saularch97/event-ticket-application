package com.example.booking.domain.entities;

import com.example.booking.controller.dto.LoginRequest;
import com.example.booking.controller.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_users")
public class User {

    public User() {
    }

    public User(UUID userId, String userName, String password, Set<Ticket> userTickets, Set<Order> orders, Set<Role> roles, Set<Event> events) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
        this.userTickets = userTickets;
        this.orders = orders;
        this.roles = roles;
        this.events = events;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(unique = true)
    private String userName;

    @Column(unique = true)
    private String password;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ticketOwner", fetch = FetchType.LAZY)
    private Set<Ticket> userTickets = new HashSet<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Order> orders = new HashSet<>();

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "eventOwner", fetch = FetchType.LAZY)
    private Set<Event> events = new HashSet<>();

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<Role> getRoles() {
        return roles;
    }
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<Ticket> getUserTickets() {
        return userTickets;
    }

    public void setUserTickets(Set<Ticket> userTickets) {
        this.userTickets = userTickets;
    }

    public Set<Order> getOrders() {
        return orders;
    }

    public void setOrders(Set<Order> orders) {
        this.orders = orders;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    public boolean isLoginCorrect(LoginRequest loginRequest, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(loginRequest.password(), this.password);
    }

    public UserDto toUserDto() {
        return new UserDto(
              userId,
              userName
        );
    }
}
