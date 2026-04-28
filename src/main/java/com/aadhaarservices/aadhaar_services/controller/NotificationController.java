package com.aadhaarservices.aadhaar_services.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Notification;
import com.aadhaarservices.aadhaar_services.repository.UserRepository;
import com.aadhaarservices.aadhaar_services.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

    @Autowired
    private UserRepository userRepository;

    // ── User: get own notifications ───────────────────────────────────
    @GetMapping
    public List<Notification> getAll(@AuthenticationPrincipal User user) {
        return service.getUserNotifications(user.getId());
    }

    @GetMapping("/unseen-count")
    public long unseenCount(@AuthenticationPrincipal User user) {
        return service.getUnseenCount(user.getId());
    }

    @PostMapping("/mark-seen/{id}")
    public void markSeen(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        service.markAsSeen(id, user.getId());
    }

    @PostMapping("/mark-all-seen")
    public void markAll(@AuthenticationPrincipal User user) {
        service.markAllAsSeen(user.getId());
    }

    // ── Admin: send notification to a specific user ───────────────────
    @PostMapping("/admin/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminSendNotification(
            @RequestBody Map<String, Object> body) {

        Object userIdObj = body.get("userId");
        String title   = (String) body.get("title");
        String message = (String) body.get("message");

        if (userIdObj == null || title == null || message == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "userId, title and message are required"));
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId"));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        service.create(userId, title, message);

        return ResponseEntity.ok(Map.of(
                "message", "Notification sent to user " + userOpt.get().getUsername(),
                "userId", userId
        ));
    }

    // ── Admin: broadcast notification to ALL users ────────────────────
    @PostMapping("/admin/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminBroadcast(@RequestBody Map<String, String> body) {
        String title   = body.get("title");
        String message = body.get("message");

        if (title == null || message == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "title and message are required"));
        }

        List<User> allUsers = userRepository.findAll();
        allUsers.forEach(u -> service.create(u.getId(), title, message));

        return ResponseEntity.ok(Map.of(
                "message", "Broadcast sent to " + allUsers.size() + " users"
        ));
    }
}