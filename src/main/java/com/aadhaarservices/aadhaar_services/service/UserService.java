package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Get User profile by username
    public User getUserProfile(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return optionalUser.get();
    }

    // Upload User profile photo
    public String uploadProfilePhoto(User user, MultipartFile file) throws IOException {
        String uploadDir = "uploads/";
        Files.createDirectories(Paths.get(uploadDir));

        String fileName = user.getId() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        user.setProfilePhoto("/uploads/" + fileName);  // Path relative to public folder
        userRepository.save(user);

        return "/uploads/" + fileName; // Return file path
    }

    // Update User profile information (for updating details like name, phone, etc.)
    public User updateUserProfile(UserDetails userDetails, User updatedUser) {
        User user = getUserProfile(userDetails.getUsername()); // Fetch existing user by username

        // Update user fields
        user.setFullName(updatedUser.getFullName());
        user.setAadhaarNumber(updatedUser.getAadhaarNumber());
        user.setEmail(updatedUser.getEmail());
        user.setPhone(updatedUser.getPhone());
        user.setAddress(updatedUser.getAddress());
        user.setTwoFA(updatedUser.isTwoFA());
        user.setAadhaarVerified(updatedUser.isAadhaarVerified());
        user.setPanVerified(updatedUser.isPanVerified());

        return userRepository.save(user); // Save the updated user
    }

    // Get User by username for profile display or admin use
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with this username " + username));
    }
    public void saveUser(User user) {
        userRepository.save(user);
    }

}
