package com.aadhaarservices.aadhaar_services.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aadhaarservices.aadhaar_services.model.Notification;
import com.aadhaarservices.aadhaar_services.repository.NotificationRepository;

@Service
public class NotificationService {

    @Autowired
    NotificationRepository repo;

    public List<Notification> getUserNotifications(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnseenCount(Long userId) {
        return repo.countByUserIdAndSeenFalse(userId);
    }

    public void markAsSeen(Long id, Long userId) {
        Notification n = repo.findById(id).orElseThrow();
        if (n.getUserId().equals(userId)) {
            n.setSeen(true);
            repo.save(n);
        }
    }

    public void markAllAsSeen(Long userId) {
        List<Notification> list = repo.findByUserIdOrderByCreatedAtDesc(userId);
        list.forEach(n -> n.setSeen(true));
        repo.saveAll(list);
    }

    // 🔥 NEW: Create Notification
    public void create(Long userId, String title, String message) {
        Notification n = new Notification(userId, title, message);
        repo.save(n);
    }
}

