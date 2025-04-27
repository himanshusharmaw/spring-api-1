package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.service.WalletService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @PostMapping("/add-funds")
    public ResponseEntity<String> addFunds(
        @AuthenticationPrincipal User user,
        @RequestParam BigDecimal amount
    ) {
        walletService.addFunds(user, amount);
        return ResponseEntity.ok("Funds added successfully");
    }

    @PostMapping("/deduct-funds")
    public ResponseEntity<String> deductFunds(
        @AuthenticationPrincipal User user,
        @RequestParam BigDecimal amount
    ) {
        try {
            walletService.deductFunds(user, amount);
            return ResponseEntity.ok("Funds deducted successfully");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .badRequest()
                .body("Insufficient balance: " + ex.getMessage());
        }
    }
}
