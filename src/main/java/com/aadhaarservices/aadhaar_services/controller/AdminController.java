package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.config.JwtUtils;
import com.aadhaarservices.aadhaar_services.model.PaymentDetails;
import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Wallet;
import com.aadhaarservices.aadhaar_services.repository.PaymentDetailsRepository;
import com.aadhaarservices.aadhaar_services.repository.UserRepository;
import com.aadhaarservices.aadhaar_services.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PaymentDetailsRepository paymentDetailsRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtil;


    private static final String ADMIN_SECRET_KEY = "ADMIN_SECRET_123";

    @Autowired
    public AdminController(UserRepository userRepository, WalletRepository walletRepository, PaymentDetailsRepository paymentDetailsRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.paymentDetailsRepository = paymentDetailsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Admin login (verify username, password, and secret key)
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest loginRequest) {
        if (!ADMIN_SECRET_KEY.equals(loginRequest.getSecretKey())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Invalid secret key"));
        }

        User admin = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(admin);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", admin.getUsername(),
                "role", "ADMIN"
        ));
    }

    // Create new user with wallet
    @PostMapping("/users/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody User user) {
        // Create the user and encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        // Create an empty wallet for the user and associate it
        Wallet wallet = new Wallet();
        wallet.setUser(user);  // Associate wallet with user
        walletRepository.save(wallet); // Save wallet

        return ResponseEntity.ok("User and wallet created successfully");
    }

    // Update user Aadhaar profile details
    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        User user = userRepository.findById(id).orElseThrow();
        user.setFullName(updatedUser.getFullName());
        user.setAadhaarNumber(updatedUser.getAadhaarNumber());
        user.setProfilePhoto(updatedUser.getProfilePhoto());  // Allow updating of profile photo
        userRepository.save(user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);	
        return ResponseEntity.ok("User deleted");
    }

    // Get wallet details of a user by userId
    @GetMapping("/wallet/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Wallet> getWalletDetails(@PathVariable Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        return ResponseEntity.ok(wallet);
    }

    // Update wallet balance for a user by userId
    @PutMapping("/wallet/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateWalletBalance(@PathVariable Long userId, @RequestBody BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
        wallet.setBalance(wallet.getBalance().add(amount)); // Adding the amount to the balance
        walletRepository.save(wallet);
        return ResponseEntity.ok("Wallet balance updated");
    }

    // Create a new payment for a user
    @PostMapping("/payments/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createPayment(@RequestBody PaymentDetails paymentDetails) {
        paymentDetailsRepository.save(paymentDetails);

        // Update user's wallet balance based on the payment amount
        Wallet wallet = walletRepository.findByUserId(paymentDetails.getUser().getId()).orElseThrow();
        wallet.setBalance(wallet.getBalance().add(paymentDetails.getAmount()));  // Add payment to balance
        walletRepository.save(wallet);

        return ResponseEntity.ok("Payment created successfully");
    }

    // Get all payment details
    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentDetails>> getAllPayments() {
        return ResponseEntity.ok(paymentDetailsRepository.findAll());
    }

    // Update payment status by paymentId
    @PutMapping("/payments/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updatePaymentStatus(@PathVariable Long paymentId, @RequestBody String status) {
        PaymentDetails payment = paymentDetailsRepository.findById(paymentId).orElseThrow();
        payment.setStatus(status);
        paymentDetailsRepository.save(payment);
        return ResponseEntity.ok("Payment status updated");
    }

    // Inner class for Admin Login Request
    public static class AdminLoginRequest {
        private String username;
        private String password;
        private String secretKey;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    }
}
