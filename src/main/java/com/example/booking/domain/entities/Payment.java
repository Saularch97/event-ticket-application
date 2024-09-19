package com.example.booking.domain.entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {

    public Payment() {
    }

    public Payment(UUID id, String userEmail, double amount) {
        this.id = id;
        this.userEmail = userEmail;
        this.amount = amount;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id")
    private UUID id;

    @Column(name="user_email")
    private String userEmail;

    @Column(name = "amount")
    private double amount;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
