package com.aadhaarservices.aadhaar_services.repository;
import com.aadhaarservices.aadhaar_services.model.Notification;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndSeenFalse(Long userId);
}
