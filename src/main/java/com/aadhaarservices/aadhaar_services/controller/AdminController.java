package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.config.JwtUtils;
import com.aadhaarservices.aadhaar_services.model.PaymentDetails;
import com.aadhaarservices.aadhaar_services.model.Transaction;
import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Wallet;
import com.aadhaarservices.aadhaar_services.repository.PaymentDetailsRepository;
import com.aadhaarservices.aadhaar_services.repository.TransactionRepository;
import com.aadhaarservices.aadhaar_services.repository.UserRepository;
import com.aadhaarservices.aadhaar_services.repository.WalletRepository;
import com.aadhaarservices.aadhaar_services.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PaymentDetailsRepository paymentDetailsRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;

    @Autowired
    private JwtUtils jwtUtil;

    @Autowired
    private NotificationService notificationService;

    private static final String ADMIN_SECRET_KEY = "ADMIN_SECRET_123";
    private static final BigDecimal NEW_USER_BONUS = new BigDecimal("75");

    @Autowired
    public AdminController(UserRepository userRepository,
                           WalletRepository walletRepository,
                           PaymentDetailsRepository paymentDetailsRepository,
                           PasswordEncoder passwordEncoder,
                           TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.paymentDetailsRepository = paymentDetailsRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionRepository = transactionRepository;
    }

    // ─────────────────────────────────────────────────────────────────
    // AUTH
    // ─────────────────────────────────────────────────────────────────

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

        if (!"ADMIN".equals(admin.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not an admin account"));
        }

        String token = jwtUtil.generateToken(admin);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", admin.getUsername(),
                "role", "ADMIN"
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // USER – LIST
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();

        List<Map<String, Object>> userData = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("fullName", user.getFullName());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone());
            map.put("role", user.getRole());
            map.put("address", user.getAddress());
            map.put("aadhaarNumber", user.getAadhaarNumber());
            map.put("dateOfBirth", user.getDateOfBirth());
            map.put("registrationDate", user.getRegistrationDate());
            map.put("accountLocked", user.getAccountLocked());
            map.put("aadhaarVerified", user.isAadhaarVerified());
            map.put("panVerified", user.getPanVerified());
            map.put("emailVerified", user.getEmailVerified());
            map.put("mobileVerified", user.getMobileVerified());
            map.put("aadhaarLinked", user.getAadhaarLinked());
            map.put("twoFA", user.isTwoFA());
            map.put("lastLogin", user.getLastLogin());
            map.put("bankName", user.getBankName());
            map.put("accountNumber", user.getAccountNumber());
            map.put("ifscCode", user.getIfscCode());
            map.put("branch", user.getBranch());

            if (user.getWallet() != null) {
                map.put("wallet", Map.of(
                        "balance", user.getWallet().getBalance(),
                        "status", user.getWallet().getStatus() != null ? user.getWallet().getStatus() : "INACTIVE"
                ));
            } else {
                map.put("wallet", Map.of("balance", 0, "status", "INACTIVE"));
            }
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(userData);
    }

    // ─────────────────────────────────────────────────────────────────
    // USER – SINGLE
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserWithWallet(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        Optional<Wallet> walletOpt = walletRepository.findByUserId(id);

        Map<String, Object> response = new HashMap<>();
        response.put("user", userOpt.get());
        response.put("wallet", walletOpt.orElse(null));

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────
    // USER – CREATE  (auto-credit ₹75 bonus)
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/users/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody User user) {

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        if (user.getAadhaarNumber() != null
                && !user.getAadhaarNumber().isEmpty()
                && user.getAadhaarNumber().length() != 12) {
            return ResponseEntity.badRequest().body("Invalid Aadhaar number (must be 12 digits)");
        }

        user.setRole("USER");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRegistrationDate(String.valueOf(java.time.LocalDate.now()));
        user.setAccountLocked(false);
        user.setTwoFA(false);
        user.setAadhaarVerified(false);
        user.setPanVerified(false);
        user.setEmailVerified(false);
        user.setMobileVerified(false);
        user.setAadhaarLinked(false);

        User savedUser = userRepository.save(user);

        // Create wallet with ₹75 signup bonus
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(NEW_USER_BONUS);
        wallet.setStatus("inactive");
        walletRepository.save(wallet);

        // Record bonus transaction
        Transaction bonusTxn = new Transaction();
        bonusTxn.setUser(savedUser);
        bonusTxn.setAmount(NEW_USER_BONUS);
        bonusTxn.setType("ADD_FUNDS");
        bonusTxn.setStatus("SUCCESS");
        bonusTxn.setDescription("₹75 signup bonus credited by admin.");
        bonusTxn.setReferenceId("BONUS-" + System.currentTimeMillis());
        transactionRepository.save(bonusTxn);

        // Notification
        notificationService.create(
                savedUser.getId(),
                "Account Created",
                "Your account has been successfully created. ₹75 signup bonus has been added to your wallet."
        );

        return ResponseEntity.ok(Map.of(
                "message", "User created successfully with ₹75 signup bonus",
                "userId", savedUser.getId()
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // USER – EDIT (all User model fields)
    // ─────────────────────────────────────────────────────────────────

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        User user = userOpt.get();

        // Basic info
        if (data.containsKey("username"))       user.setUsername((String) data.get("username"));
        if (data.containsKey("fullName"))        user.setFullName((String) data.get("fullName"));
        if (data.containsKey("email"))           user.setEmail((String) data.get("email"));
        if (data.containsKey("phone"))           user.setPhone((String) data.get("phone"));
        if (data.containsKey("address"))         user.setAddress((String) data.get("address"));
        if (data.containsKey("dateOfBirth"))     user.setDateOfBirth((String) data.get("dateOfBirth"));
        if (data.containsKey("aadhaarNumber"))   user.setAadhaarNumber((String) data.get("aadhaarNumber"));
        if (data.containsKey("profilePhoto"))    user.setProfilePhoto((String) data.get("profilePhoto"));
        if (data.containsKey("recentActivity"))  user.setRecentActivity((String) data.get("recentActivity"));
        if (data.containsKey("role"))            user.setRole((String) data.get("role"));

        // Bank details
        if (data.containsKey("bankName"))        user.setBankName((String) data.get("bankName"));
        if (data.containsKey("accountNumber"))   user.setAccountNumber((String) data.get("accountNumber"));
        if (data.containsKey("ifscCode"))        user.setIfscCode((String) data.get("ifscCode"));
        if (data.containsKey("branch"))          user.setBranch((String) data.get("branch"));

        // Boolean flags
        if (data.containsKey("twoFA"))           user.setTwoFA(Boolean.TRUE.equals(data.get("twoFA")));
        if (data.containsKey("aadhaarVerified")) user.setAadhaarVerified(Boolean.TRUE.equals(data.get("aadhaarVerified")));
        if (data.containsKey("panVerified"))     user.setPanVerified(Boolean.TRUE.equals(data.get("panVerified")));
        if (data.containsKey("emailVerified"))   user.setEmailVerified(Boolean.TRUE.equals(data.get("emailVerified")));
        if (data.containsKey("mobileVerified"))  user.setMobileVerified(Boolean.TRUE.equals(data.get("mobileVerified")));
        if (data.containsKey("aadhaarLinked"))   user.setAadhaarLinked(Boolean.TRUE.equals(data.get("aadhaarLinked")));
        if (data.containsKey("accountLocked"))   user.setAccountLocked(Boolean.TRUE.equals(data.get("accountLocked")));

        // Password (only if provided and non-empty)
        String newPassword = (String) data.get("password");
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);

        // Update wallet balance if provided
        Optional<Wallet> walletOpt = walletRepository.findByUserId(id);
        if (walletOpt.isPresent() && data.containsKey("walletBalance")) {
            Wallet wallet = walletOpt.get();
            Object balObj = data.get("walletBalance");
            if (balObj instanceof Number) wallet.setBalance(new BigDecimal(((Number) balObj).doubleValue()));
            else if (balObj instanceof String) wallet.setBalance(new BigDecimal((String) balObj));
            walletRepository.save(wallet);
        }

        return ResponseEntity.ok(Map.of("message", "User updated successfully"));
    }

    // ─────────────────────────────────────────────────────────────────
    // USER – DELETE
    // ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(404).body("User not found");
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted");
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN ADD FUND TO USER WALLET
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/users/{id}/add-fund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminAddFund(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        Optional<Wallet> walletOpt = walletRepository.findByUserId(id);
        if (walletOpt.isEmpty()) return ResponseEntity.status(404).body("Wallet not found");

        User user = userOpt.get();
        Wallet wallet = walletOpt.get();

        Object amtObj = body.get("amount");
        if (amtObj == null) return ResponseEntity.badRequest().body("Amount is required");

        BigDecimal amount;
        try {
            amount = new BigDecimal(amtObj.toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0)
                return ResponseEntity.badRequest().body("Amount must be positive");
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }

        String note = body.containsKey("note") ? (String) body.get("note") : "Funds added by admin";

        wallet.setBalance(wallet.getBalance().add(amount));
        if ("INACTIVE".equals(wallet.getStatus()) || wallet.getStatus() == null) {
            wallet.setStatus("ACTIVE");
        }
        walletRepository.save(wallet);

        // Transaction record
        Transaction txn = new Transaction();
        txn.setUser(user);
        txn.setAmount(amount);
        txn.setType("ADD_FUNDS");
        txn.setStatus("SUCCESS");
        txn.setDescription(note);
        txn.setReferenceId("ADM-" + System.currentTimeMillis());
        transactionRepository.save(txn);

        // Notification to user
        notificationService.create(
                user.getId(),
                "Funds Added",
                "₹" + amount + " has been added to your wallet by admin. Note: " + note
        );

        return ResponseEntity.ok(Map.of(
                "message", "₹" + amount + " added to " + user.getUsername() + "'s wallet",
                "newBalance", wallet.getBalance(),
                "walletStatus", wallet.getStatus()
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // WALLET – CHANGE STATUS
    // ─────────────────────────────────────────────────────────────────

    @PutMapping("/wallet/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateWalletStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        List<String> allowed = List.of("ACTIVE", "INACTIVE", "SUSPENDED", "DISABLED");
        if (status == null || !allowed.contains(status)) {
            return ResponseEntity.badRequest()
                    .body("Invalid status. Allowed: ACTIVE, INACTIVE, SUSPENDED, DISABLED");
        }

        Optional<Wallet> walletOpt = walletRepository.findByUserId(userId);
        if (walletOpt.isEmpty()) return ResponseEntity.status(404).body("Wallet not found");

        Wallet wallet = walletOpt.get();
        String oldStatus = wallet.getStatus();
        wallet.setStatus(status);
        walletRepository.save(wallet);

        // Notify user
        notificationService.create(
                userId,
                "Wallet Status Updated",
                "Your wallet status has been changed from " + oldStatus + " to " + status + " by admin."
        );

        return ResponseEntity.ok(Map.of(
                "message", "Wallet status updated to " + status,
                "userId", userId,
                "status", status
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // PAYMENT – CREATE
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/payments/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createPayment(@RequestBody PaymentDetails paymentDetails) {
        paymentDetailsRepository.save(paymentDetails);

        Optional<Wallet> walletOpt = walletRepository.findByUserId(paymentDetails.getUser().getId());
        if (walletOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Wallet not found for the user");
        }

        Wallet wallet = walletOpt.get();
        wallet.setBalance(wallet.getBalance().add(paymentDetails.getAmount()));
        walletRepository.save(wallet);

        return ResponseEntity.ok("Payment created successfully");
    }

    // ─────────────────────────────────────────────────────────────────
    // PAYMENT – LIST
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentDetails>> getAllPayments() {
        return ResponseEntity.ok(paymentDetailsRepository.findAll());
    }

    @PutMapping("/payments/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updatePaymentStatus(
            @PathVariable Long paymentId,
            @RequestBody String status) {
        Optional<PaymentDetails> paymentOpt = paymentDetailsRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) return ResponseEntity.status(404).body("Payment not found");

        PaymentDetails payment = paymentOpt.get();
        payment.setStatus(status);
        paymentDetailsRepository.save(payment);
        return ResponseEntity.ok("Payment status updated");
    }

    // ─────────────────────────────────────────────────────────────────
    // FUND TRANSACTIONS – All ADD_FUNDS across all users (for Payment tab)
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/transactions/funds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllFundTransactions() {
        List<Transaction> txns = transactionRepository.findByTypeOrderByCreatedAtDesc("ADD_FUNDS");

        List<Map<String, Object>> result = txns.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("userId", t.getUser().getId());
            map.put("username", t.getUser().getUsername());
            map.put("fullName", t.getUser().getFullName());
            map.put("amount", t.getAmount());
            map.put("status", t.getStatus());
            map.put("description", t.getDescription());
            map.put("referenceId", t.getReferenceId());
            map.put("createdAt", t.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────
    // INNER CLASS
    // ─────────────────────────────────────────────────────────────────

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