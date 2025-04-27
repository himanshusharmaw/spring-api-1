package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Wallet;
import com.aadhaarservices.aadhaar_services.payload.ProfileResponse;
import com.aadhaarservices.aadhaar_services.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            User user = userService.getUserProfile(userDetails.getUsername());
            System.out.println(">> Profile request for: " + userDetails.getUsername());

            // If wallet is null, create it with ₹50 balance
            if (user.getWallet() == null) {
                Wallet wallet = new Wallet();
                wallet.setBalance(new java.math.BigDecimal("50.00"));
                wallet.setUser(user);
                user.setWallet(wallet);
                userService.saveUser(user); // Save user with new wallet
                System.out.println(">> Wallet created with ₹50 for user: " + user.getUsername());
            }

            // Construct response
            ProfileResponse profile = new ProfileResponse();
            profile.setFullName(user.getFullName() != null ? user.getFullName() : "");
            profile.setProfilePhoto(user.getProfilePhoto() != null ? user.getProfilePhoto() : "");
            profile.setAadhaarNumber(user.getAadhaarNumber() != null ? user.getAadhaarNumber() : "");
            profile.setEmail(user.getEmail() != null ? user.getEmail() : "");
            profile.setPhone(user.getPhone() != null ? user.getPhone() : "");
            profile.setAddress(user.getAddress() != null ? user.getAddress() : "");
            profile.setWalletBalance(user.getWallet() != null ? user.getWallet().getBalance().doubleValue() : 0.0);
            profile.setRegistrationDate(
                user.getRegistrationDate() != null ? user.getRegistrationDate() : "N/A"
            );
            profile.setTwoFA(user.isTwoFA());
            profile.setLastLogin(
                user.getLastLogin() != null ? user.getLastLogin().toString() : "Never"
            );
            profile.setAadhaarVerified(user.isAadhaarVerified());
            profile.setPanVerified(user.isPanVerified());

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            System.out.println(">> Error fetching profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }
    
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        try {
            User user = userService.getUserProfile(userDetails.getUsername());
            String filePath = userService.uploadProfilePhoto(user, file);
            return ResponseEntity.ok("Photo uploaded successfully: " + filePath);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Photo upload failed");
        }
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            return ResponseEntity.ok(user); // Return the User object as part of the response
        } catch (UsernameNotFoundException e) {
            // Return a response with a NOT_FOUND status and a message
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + username);
        }
    }

}
