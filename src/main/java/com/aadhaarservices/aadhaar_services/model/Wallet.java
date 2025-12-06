package com.aadhaarservices.aadhaar_services.model;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal balance = BigDecimal.ZERO;

    private String status = "INACTIVE"; 
    // Possible values: ACTIVE, INACTIVE, DISABLED

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonBackReference
    private User user;

    public Wallet() {}

    public Wallet(BigDecimal balance, User user) {
        this.balance = balance;
        this.user = user;
        this.status = "INACTIVE";
    }

    // ---------------- Getters and Setters ----------------
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public BigDecimal getBalance() { return balance; }

    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public User getUser() { return user; }

    public void setUser(User user) { this.user = user; }
}
