package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.Account;
import com.aadhaarservices.aadhaar_services.model.Payment;
import com.aadhaarservices.aadhaar_services.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    // Get account information
    @GetMapping("/{id}")
    public Account getAccountInfo(@PathVariable Long id) {
        return accountService.getAccountInfo(id);
    }

    // Update account information
    @PutMapping("/{id}")
    public Account updateAccountInfo(@PathVariable Long id,
                                     @RequestParam String accountNumber,
                                     @RequestParam String accountHolderName,
                                     @RequestParam String ifscCode,
                                     @RequestParam String bankName,
                                     @RequestParam BigDecimal totalAmount) {
        return accountService.updateAccountInfo(id, accountNumber, accountHolderName, ifscCode, bankName, totalAmount);
    }
}
