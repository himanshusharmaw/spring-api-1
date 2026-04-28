package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.MachineApplication;
import com.aadhaarservices.aadhaar_services.model.MachineTracking;
import com.aadhaarservices.aadhaar_services.repository.MachineApplicationRepo;
import com.aadhaarservices.aadhaar_services.repository.MachineTrackingRepo;
import com.aadhaarservices.aadhaar_services.service.MachineService;
import com.aadhaarservices.aadhaar_services.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/machine")
public class MachineAdminController {

    private final MachineService machineService;
    private final NotificationService notificationService;
    private final MachineApplicationRepo applicationRepo;
    private final MachineTrackingRepo trackingRepo;

    public MachineAdminController(MachineService machineService,
                                  NotificationService notificationService,
                                  MachineApplicationRepo applicationRepo,
                                  MachineTrackingRepo trackingRepo) {
        this.machineService = machineService;
        this.notificationService = notificationService;
        this.applicationRepo = applicationRepo;
        this.trackingRepo = trackingRepo;
    }

    // ── Get all applications ──────────────────────────────────────────
    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public List<MachineApplication> getAllApps() {
        return machineService.getAllApplications();
    }

    // ── Approve ───────────────────────────────────────────────────────
    @PostMapping("/approve/{appId}")
    @PreAuthorize("hasRole('ADMIN')")
    public MachineApplication approve(@PathVariable Long appId) {
        MachineApplication app = machineService.approveApplication(appId);
        notificationService.create(
                app.getUserId(),
                "Machine Application Approved",
                "Your Aadhaar machine application (ID: " + app.getId() + ") has been approved."
        );
        return app;
    }

    // ── Reject ────────────────────────────────────────────────────────
    @PostMapping("/reject/{appId}")
    @PreAuthorize("hasRole('ADMIN')")
    public MachineApplication reject(
            @PathVariable Long appId,
            @RequestBody String reason) {
        MachineApplication app = machineService.rejectApplication(appId, reason);
        notificationService.create(
                app.getUserId(),
                "Machine Application Rejected",
                "Your application (ID: " + app.getId() + ") was rejected. Reason: " + reason
        );
        return app;
    }

    // ── Delete / remove an application (e.g. after approval is done) ─
    @DeleteMapping("/applications/{appId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteApplication(@PathVariable Long appId) {
        if (!applicationRepo.existsById(appId)) {
            return ResponseEntity.status(404).body(Map.of("error", "Application not found"));
        }
        // Remove tracking rows first (FK safety)
        List<MachineTracking> tracks = trackingRepo.findByApplicationId(appId);
        trackingRepo.deleteAll(tracks);
        applicationRepo.deleteById(appId);
        return ResponseEntity.ok(Map.of("message", "Application " + appId + " removed successfully"));
    }

    // ── Add tracking update (accepts JSON with status + optional message) ─
    @PostMapping("/track/{appId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addTracking(
            @PathVariable Long appId,
            @RequestBody Map<String, String> body) {

        String status  = body.getOrDefault("status",  "Update");
        String message = body.getOrDefault("message", "");

        MachineTracking track = machineService.addAdminTrackingWithMessage(appId, status, message);

        MachineApplication app = machineService.getApplicationById(track.getApplicationId());
        notificationService.create(
                app.getUserId(),
                "Machine Application Update",
                "Your application (ID: " + app.getId() + ") has a new update: " + status
                        + (message.isBlank() ? "" : " – " + message)
        );

        return ResponseEntity.ok(track);
    }

    // ── Get tracking history for one application ──────────────────────
    @GetMapping("/track/{appId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MachineTracking>> getTracking(@PathVariable Long appId) {
        return ResponseEntity.ok(trackingRepo.findByApplicationId(appId));
    }
}