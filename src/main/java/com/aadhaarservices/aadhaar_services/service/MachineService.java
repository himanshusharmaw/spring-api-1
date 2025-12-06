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


    // USER SUBMITS APPLICATION
    public MachineApplication apply(Long userId) {

        long count = applicationRepo.countByUserId(userId);
        if (count >= 3) throw new RuntimeException("Max 3 machines allowed");

        MachineApplication app = new MachineApplication();
        app.setUserId(userId);
        app.setMachineCount(1);
        app.setStatus("SUBMITTED");
        app.setCreatedAt(LocalDateTime.now());
        applicationRepo.save(app);

        // Tracking
        addTracking(app.getId(), "Application Submitted");

        // 🔔 Notification
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


    // Reusable tracking method
    private void addTracking(Long appId, String status) {
        MachineTracking t = new MachineTracking();
        t.setApplicationId(appId);
        t.setStatus(status);
        t.setTimestamp(LocalDateTime.now());
        trackingRepo.save(t);
    }


    // ADMIN: Get all applications
    public List<MachineApplication> getAllApplications() {
        return applicationRepo.findAll();
    }


    // ADMIN: APPROVE
    public MachineApplication approveApplication(Long appId) {
        MachineApplication app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        app.setStatus("APPROVED");
        applicationRepo.save(app);

        addTracking(appId, "Application Approved by Admin");

        // 🔔 Notification
        notificationService.create(
                app.getUserId(),
                "Machine Application Approved",
                "Your machine application (ID: " + app.getId() + ") has been approved."
        );

        return app;
    }


    // ADMIN: REJECT
    public MachineApplication rejectApplication(Long appId, String reason) {
        MachineApplication app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        app.setStatus("REJECTED");
        applicationRepo.save(app);

        addTracking(appId, "Application Rejected: " + reason);

        // 🔔 Notification
        notificationService.create(
                app.getUserId(),
                "Machine Application Rejected",
                "Your application (ID: " + app.getId() + ") was rejected. Reason: " + reason
        );

        return app;
    }


    // ADMIN: Add tracking update
    public MachineTracking addAdminTracking(Long appId, String status) {

        MachineApplication app = applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        MachineTracking t = new MachineTracking();
        t.setApplicationId(appId);
        t.setStatus(status);
        t.setTimestamp(LocalDateTime.now());
        trackingRepo.save(t);

        // 🔔 Notification
        notificationService.create(
                app.getUserId(),
                "Machine Application Update",
                "New update on your application: " + status
        );

        return t;

    }
    
    public MachineApplication getApplicationById(Long appId) {
        return applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

}

