package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.Transaction;
import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Wallet;
import com.aadhaarservices.aadhaar_services.payload.ProfileResponse;
import com.aadhaarservices.aadhaar_services.service.TransactionService;
import com.aadhaarservices.aadhaar_services.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final PasswordEncoder passwordEncoder;
    private final com.aadhaarservices.aadhaar_services.repository.UserRepository userRepository;

    @Autowired
    public UserController(
            UserService userService,
            TransactionService transactionService,
            PasswordEncoder passwordEncoder,
            com.aadhaarservices.aadhaar_services.repository.UserRepository userRepository
    ) {
        this.userService = userService;
        this.transactionService = transactionService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    // ---------------------------------------------------------
    // GET USER PROFILE
    // ---------------------------------------------------------
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            User fullUser = userService.getUserProfile(user.getUsername());

            // Auto-create wallet if missing
            if (fullUser.getWallet() == null) {
                Wallet wallet = new Wallet();
                wallet.setBalance(new java.math.BigDecimal("75.00"));
                wallet.setUser(fullUser);
                fullUser.setWallet(wallet);
                userService.saveUser(fullUser);
            }

            ProfileResponse profile = new ProfileResponse();

            profile.setFullName(fullUser.getFullName());
            profile.setProfilePhoto(fullUser.getProfilePhoto());
            profile.setAadhaarNumber(fullUser.getAadhaarNumber());
            profile.setEmail(fullUser.getEmail());
            profile.setPhone(fullUser.getPhone());
            profile.setAddress(fullUser.getAddress());

            profile.setWalletBalance(fullUser.getWalletBalance());
            profile.setWalletStatus(
            	    fullUser.getWallet() != null ? fullUser.getWallet().getStatus() : "NOT_CREATED"
            	);

            profile.setRegistrationDate(fullUser.getRegistrationDate());

            profile.setTwoFA(fullUser.isTwoFA());
            profile.setLastLogin(fullUser.getLastLogin() != null ? fullUser.getLastLogin().toString() : "Never");

            profile.setAadhaarVerified(fullUser.isAadhaarVerified());
            profile.setPanVerified(fullUser.getPanVerified());

            profile.setEmailVerified(fullUser.getEmailVerified());
            profile.setMobileVerified(fullUser.getMobileVerified());
            profile.setAadhaarLinked(fullUser.getAadhaarLinked());
            profile.setAccountLocked(fullUser.getAccountLocked());

            profile.setRecentActivity(fullUser.getRecentActivity());
            profile.setDateOfBirth(fullUser.getDateOfBirth());

            profile.setBankName(fullUser.getBankName());
            profile.setAccountNumber(fullUser.getAccountNumber());
            profile.setIfscCode(fullUser.getIfscCode());
            profile.setBranch(fullUser.getBranch());

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // UPLOAD PROFILE PHOTO
    // ---------------------------------------------------------
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
            @RequestParam("file") MultipartFile file) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        // Fetch your real entity user
        User user = userService.getUserByUsername(principal.getUsername());

        try {
            String filePath = userService.uploadProfilePhoto(user, file);
            return ResponseEntity.ok("Photo uploaded successfully: " + filePath);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Photo upload failed");
        }
    }

    // ---------------------------------------------------------
    // GET USER BY USERNAME
    // ---------------------------------------------------------
    @GetMapping("/{username}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user);

        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User not found: " + username);
        }
    }

    // ---------------------------------------------------------
    // GET TRANSACTION HISTORY
    // ---------------------------------------------------------
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactionHistory(@AuthenticationPrincipal User user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        List<Transaction> transactions = transactionService.getTransactions(user);
        return ResponseEntity.ok(transactions);
    }

    // ---------------------------------------------------------
    // UPDATE USER SETTINGS
    // ---------------------------------------------------------
    public void updateUserSettings(User user, Map<String, String> payload) {
        user.setEmail(payload.get("email"));
        user.setPhone(payload.get("phone"));
        user.setAddress(payload.get("address"));

        // Bank info
        user.setBankName(payload.get("bankName"));
        user.setAccountNumber(payload.get("accountNumber"));
        user.setIfscCode(payload.get("ifscCode"));
        user.setBranch(payload.get("branch"));

        // Password change
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");

        if (newPassword != null && !newPassword.isEmpty()) {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
    }
    
 // ---------------------------------------------------------
 // UPDATE USER SETTINGS (PUT)
 // ---------------------------------------------------------
 @PutMapping("/settings")
 public ResponseEntity<?> updateSettings(
         @AuthenticationPrincipal User loggedUser,
         @RequestBody Map<String, String> payload
 ) {
     if (loggedUser == null) {
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
     }

     try {
         User user = userService.getUserByUsername(loggedUser.getUsername());
         updateUserSettings(user, payload);
         return ResponseEntity.ok("Settings updated successfully");

     } catch (IllegalArgumentException ex) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());

     } catch (Exception ex) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body("Failed to update settings: " + ex.getMessage());
     }
 }

}
