package com.aadhaarservices.aadhaar_services.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Notification;
import com.aadhaarservices.aadhaar_services.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService service;

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
}
