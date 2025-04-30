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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
            return ResponseEntity.status(403).body(Map.of("error", "Invalid secret key"));
        }

        Optional<User> adminOpt = userRepository.findByUsername(loginRequest.getUsername());
        if (adminOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin not found"));
        }

        User admin = adminOpt.get();
        if (!passwordEncoder.matches(loginRequest.getPassword(), admin.getPassword())) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtUtil.generateToken(admin);
        return ResponseEntity.ok(Map.of("token", token, "username", admin.getUsername(), "role", "ADMIN"));
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
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updatedData) {
        User user = userRepository.findById(id).orElseThrow();
        Wallet wallet = walletRepository.findByUserId(id).orElseThrow();

        // Update user fields
        user.setUsername((String) updatedData.get("username"));
        user.setFullName((String) updatedData.get("fullName"));
        user.setAadhaarNumber((String) updatedData.get("aadhaarNumber"));
        user.setPhone((String) updatedData.get("mobileNumber"));
        user.setEmail((String) updatedData.get("email"));
        user.setProfilePhoto((String) updatedData.get("profilePhoto"));
        user.setAddress((String) updatedData.get("address"));  // Update address
        user.setTwoFA(updatedData.get("twoFA") != null ? (Boolean) updatedData.get("twoFA") : false); // Update twoFA status
        user.setAadhaarVerified((Boolean) updatedData.get("aadhaarVerified")); // Update aadhaarVerified

        // Parse wallet balance if present
        Object balanceObj = updatedData.get("walletBalance");
        if (balanceObj instanceof Number) {
            wallet.setBalance(new BigDecimal(((Number) balanceObj).doubleValue()));
        } else if (balanceObj instanceof String) {
            wallet.setBalance(new BigDecimal((String) balanceObj));
        }   

        // Save updated user and wallet
        userRepository.save(user);
        walletRepository.save(wallet);

        return ResponseEntity.ok("User and wallet updated successfully");
    }
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> simplifiedUsers = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(simplifiedUsers);
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserWithWallet(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Optional<Wallet> walletOpt = walletRepository.findByUserId(id);
        if (walletOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Wallet not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("user", userOpt.get());
        response.put("wallet", walletOpt.get());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body("User not found");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }

    // Create a new payment for a user
    @PostMapping("/payments/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createPayment(@RequestBody PaymentDetails paymentDetails) {
        paymentDetailsRepository.save(paymentDetails);

        // Update user's wallet balance based on the payment amount
        Optional<Wallet> walletOpt = walletRepository.findByUserId(paymentDetails.getUser().getId());
        if (walletOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Wallet not found for the user");
        }
        
        Wallet wallet = walletOpt.get();
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
        Optional<PaymentDetails> paymentOpt = paymentDetailsRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Payment not found");
        }

        PaymentDetails payment = paymentOpt.get();
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