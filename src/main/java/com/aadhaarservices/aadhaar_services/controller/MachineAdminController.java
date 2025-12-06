package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.MachineApplication;
import com.aadhaarservices.aadhaar_services.model.MachineTracking;
import com.aadhaarservices.aadhaar_services.service.MachineService;
import com.aadhaarservices.aadhaar_services.service.NotificationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/machine")
public class MachineAdminController {

    private final MachineService machineService;
    private final NotificationService notificationService;

    public MachineAdminController(MachineService machineService, NotificationService notificationService) {
        this.machineService = machineService;
        this.notificationService = notificationService;
    }

    // Get all applications
    @GetMapping("/applications")
    public List<MachineApplication> getAllApps() {
        return machineService.getAllApplications();
    }

    // Approve application
    @PostMapping("/approve/{appId}")
    public MachineApplication approve(@PathVariable Long appId) {

        MachineApplication app = machineService.approveApplication(appId);

        // 🔔 Send notification to user
        notificationService.create(
                app.getUserId(),
                "Machine Application Approved",
                "Your Aadhaar machine application (ID: " + app.getId() + ") has been approved."
        );

        return app;
    }

    // Reject application
    @PostMapping("/reject/{appId}")
    public MachineApplication reject(
            @PathVariable Long appId,
            @RequestBody String reason
    ) {
        MachineApplication app = machineService.rejectApplication(appId, reason);

        // 🔔 Send notification
        notificationService.create(
                app.getUserId(),
                "Machine Application Rejected",
                "Your application (ID: " + app.getId() + ") was rejected. Reason: " + reason
        );

        return app;
    }

    // Add tracking update
    @PostMapping("/track/{appId}")
    public MachineTracking addTracking(
            @PathVariable Long appId,
            @RequestBody String status
    ) {
        MachineTracking track = machineService.addAdminTracking(appId, status);

        // Load full application from service
        MachineApplication app = machineService.getApplicationById(track.getApplicationId());

        // 🔔 Send notification
        notificationService.create(
                app.getUserId(),
                "Machine Application Update",
                "Your application (ID: " + app.getId() + ") has a new update: " + status
        );

        return track;
    }
}
