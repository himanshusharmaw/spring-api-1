package com.aadhaarservices.aadhaar_services.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aadhaarservices.aadhaar_services.model.MachineApplication;
import com.aadhaarservices.aadhaar_services.model.MachineTracking;
import com.aadhaarservices.aadhaar_services.repository.MachineApplicationRepo;
import com.aadhaarservices.aadhaar_services.repository.MachineTrackingRepo;

@Service
public class MachineService {

    @Autowired
    private MachineApplicationRepo applicationRepo;

    @Autowired
    private MachineTrackingRepo trackingRepo;

    @Autowired
    private NotificationService notificationService;

    // ── User submits application ─────────────────────────────────────
    public MachineApplication apply(Long userId) {

        long count = applicationRepo.countByUserId(userId);
        if (count >= 3) throw new RuntimeException("Max 3 machines allowed");

        MachineApplication app = new MachineApplication();
        app.setUserId(userId);
        app.setMachineCount(1);
        app.setStatus("SUBMITTED");
        app.setCreatedAt(LocalDateTime.now());
        applicationRepo.save(app);

        addTrackingInternal(app.getId(), "Application Submitted", "");

        notificationService.create(
                userId,
                "Machine Application Submitted",
                "Your application has been successfully submitted."
        );

        return app;
    }

    public List<MachineApplication> getUserMachines(Long userId) {
        return applicationRepo.findByUserId(userId);
    }

    public List<MachineTracking> getTracking(Long userId) {
        MachineApplication app = applicationRepo.findTopByUserIdOrderByIdDesc(userId);
        if (app == null) return List.of();
        return trackingRepo.findByApplicationId(app.getId());
    }

    // ── Internal helper ──────────────────────────────────────────────
    private void addTrackingInternal(Long appId, String status, String message) {
        MachineTracking t = new MachineTracking();
        t.setApplicationId(appId);
        t.setStatus(status);
        t.setMessage(message);
        t.setTimestamp(LocalDateTime.now());
        trackingRepo.save(t);
    }

    // ── Admin: all applications ───────────────────────────────────────
    public List<MachineApplication> getAllApplications() {
        return applicationRepo.findAll();
    }

    // ── Admin: approve ───────────────────────────────────────────────
    public MachineApplication approveApplication(Long appId) {
        MachineApplication app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        app.setStatus("APPROVED");
        applicationRepo.save(app);

        addTrackingInternal(appId, "Application Approved by Admin", "");

        notificationService.create(
                app.getUserId(),
                "Machine Application Approved",
                "Your machine application (ID: " + app.getId() + ") has been approved."
        );

        return app;
    }

    // ── Admin: reject ────────────────────────────────────────────────
    public MachineApplication rejectApplication(Long appId, String reason) {
        MachineApplication app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        app.setStatus("REJECTED");
        applicationRepo.save(app);

        addTrackingInternal(appId, "Application Rejected: " + reason, reason);

        notificationService.create(
                app.getUserId(),
                "Machine Application Rejected",
                "Your application (ID: " + app.getId() + ") was rejected. Reason: " + reason
        );

        return app;
    }

    // ── Admin: add tracking (status + message) ───────────────────────
    public MachineTracking addAdminTracking(Long appId, String status) {
        return addAdminTrackingWithMessage(appId, status, "");
    }

    public MachineTracking addAdminTrackingWithMessage(Long appId, String status, String message) {
        MachineApplication app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        MachineTracking t = new MachineTracking();
        t.setApplicationId(appId);
        t.setStatus(status);
        t.setMessage(message != null ? message : "");
        t.setTimestamp(LocalDateTime.now());
        trackingRepo.save(t);

        notificationService.create(
                app.getUserId(),
                "Machine Application Update",
                "New update on your application: " + status
                        + (message != null && !message.isBlank() ? " – " + message : "")
        );

        return t;
    }

    public MachineApplication getApplicationById(Long appId) {
        return applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }
}