package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.config.JwtUtils;
import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.repository.UserRepository;

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
        return ResponseEntity.ok("Server is alive");
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        try {
            System.out.println("Before authentication: ");
            System.out.println("Username: " + user.getUsername());
            System.out.println("Password: " + user.getPassword());

            // Attempt authentication
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            
            // Fetch user from DB after successful authentication
            UserDetails userDetails = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            System.out.println("User details fetched: ");
            System.out.println("Username: " + userDetails.getUsername());
            System.out.println("Password: " + userDetails.getPassword());

            // Compare password manually for debugging
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean isPasswordValid = encoder.matches(user.getPassword(), userDetails.getPassword());
            System.out.println("Password valid: " + isPasswordValid);

            // Generate JWT if credentials are valid
            if (isPasswordValid) {
                String token = jwtUtils.generateToken(userDetails);
                return ResponseEntity.ok(Map.of("token", token));
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid username or password");
            }

        } catch (BadCredentialsException e) {
            // Handle invalid credentials
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid username or password");
        } catch (Exception e) {
            // Handle other exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login");
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