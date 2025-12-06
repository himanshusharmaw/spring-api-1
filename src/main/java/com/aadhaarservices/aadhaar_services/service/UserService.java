package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // -------------------- GET PROFILE BY USERNAME --------------------
    public User getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + username));
    }

    // -------------------- UPLOAD PROFILE PHOTO --------------------
    public String uploadProfilePhoto(User user, MultipartFile file) throws IOException {

        String uploadDir = "uploads/profile-photos/";

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String filename = user.getUsername() + "_" + file.getOriginalFilename();
        String fullPath = uploadDir + filename;

        FileOutputStream fos = new FileOutputStream(fullPath);
        fos.write(file.getBytes());
        fos.close();

        user.setProfilePhoto(fullPath);
        userRepository.save(user);

        return fullPath;
    }

    // -------------------- UPDATE PROFILE --------------------
    public User updateUserProfile(UserDetails userDetails, User updatedUser) {

        User user = getUserProfile(userDetails.getUsername());

        // Basic fields
        user.setFullName(updatedUser.getFullName());
        user.setAadhaarNumber(updatedUser.getAadhaarNumber());
        user.setEmail(updatedUser.getEmail());
        user.setPhone(updatedUser.getPhone());
        user.setAddress(updatedUser.getAddress());

        // New fields
        user.setTwoFA(updatedUser.isTwoFA());
        user.setAadhaarVerified(updatedUser.isAadhaarVerified());
        user.setPanVerified(updatedUser.getPanVerified());
        user.setEmailVerified(updatedUser.getEmailVerified());
        user.setMobileVerified(updatedUser.getMobileVerified());
        user.setAadhaarLinked(updatedUser.getAadhaarLinked());
        user.setAccountLocked(updatedUser.getAccountLocked());
        user.setRecentActivity(updatedUser.getRecentActivity());
        user.setDateOfBirth(updatedUser.getDateOfBirth());

        return userRepository.save(user);
    }

    // -------------------- GET USER BY USERNAME --------------------
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No user found with this username " + username));
    }

    // -------------------- SAVE USER --------------------
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
