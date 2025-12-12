package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.config.JwtUtils;
import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;
    
    @GetMapping("/ping")
    public ResponseEntity<String> keepAlive() {
    	System.out.println("alive");
        return ResponseEntity.ok("Server is alive");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );

            // Fetch actual user from DB
            User dbUser = userRepository.findByUsername(user.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Validate password
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean isPasswordValid = encoder.matches(user.getPassword(), dbUser.getPassword());

            if (!isPasswordValid) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid username or password");
            }

            // ✅ UPDATE LAST LOGIN HERE
            dbUser.setLastLogin(LocalDateTime.now());
            userRepository.save(dbUser);

            // Generate JWT token
            String token = jwtUtils.generateToken(dbUser);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "lastLogin", dbUser.getLastLogin().toString()
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid username or password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login error: " + e.getMessage());
        }
    }


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword())); // Encrypt password
        user.setRole("USER"); // Default role
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

}