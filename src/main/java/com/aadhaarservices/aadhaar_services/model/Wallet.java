package com.aadhaarservices.aadhaar_services.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

@Entity
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal balance = BigDecimal.ZERO;  // The balance of the wallet, default to zero

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")  // Join with the User entity by ID
    @JsonBackReference
    private User user;

    // ---------------- Constructors ----------------
    public Wallet() {} // Default constructor for JPA

    public Wallet(BigDecimal balance, User user) {
        this.balance = balance;
        this.user = user;
    }

    // ---------------- Getters and Setters ----------------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
