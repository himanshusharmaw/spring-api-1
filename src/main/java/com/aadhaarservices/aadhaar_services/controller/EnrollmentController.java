package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.Enrollment;
import com.aadhaarservices.aadhaar_services.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/enrollment")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    // ────────────────────────────────────────────────────────────────────────
    // POST /api/enrollment/submit
    // Submit a new enrollment application (Step 1-2 data: personal + address)
    // ────────────────────────────────────────────────────────────────────────
    @PostMapping("/submit")
    public ResponseEntity<?> submitEnrollment(@RequestBody Enrollment enrollment) {
        try {
            // Validate required fields
            if (enrollment.getFullNameEn() == null || enrollment.getFullNameEn().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Full name in English is required"));
            }
            if (enrollment.getMobileNumber() == null || enrollment.getMobileNumber().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mobile number is required"));
            }
            if (enrollment.getDateOfBirth() == null || enrollment.getDateOfBirth().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Date of birth is required"));
            }
            if (!enrollment.isDeclarationAccepted()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Declaration must be accepted"));
            }

            // Check duplicate mobile
            if (enrollmentService.mobileAlreadyEnrolled(enrollment.getMobileNumber())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "This mobile number is already enrolled"));
            }

            Enrollment saved = enrollmentService.createEnrollment(enrollment);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Enrollment submitted successfully",
                    "enrollmentId", saved.getEnrollmentId(),
                    "status", saved.getStatus()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Enrollment failed: " + e.getMessage()));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // POST /api/enrollment/{enrollmentId}/upload/{docType}
    // Upload a scanned document. docType: POI | POA | DOB | PHOTO
    // ────────────────────────────────────────────────────────────────────────
    @PostMapping("/{enrollmentId}/upload/{docType}")
    public ResponseEntity<?> uploadDocument(
            @PathVariable String enrollmentId,
            @PathVariable String docType,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        // Validate file type (accept PDF, JPG, PNG)
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("application/pdf")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/png"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF, JPG, and PNG files are accepted"));
        }

        // Validate file size (max 5 MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File size must be less than 5 MB"));
        }

        try {
            String filePath = enrollmentService.uploadDocument(enrollmentId, docType.toUpperCase(), file);
            return ResponseEntity.ok(Map.of(
                    "message", "Document uploaded successfully",
                    "filePath", filePath,
                    "docType", docType.toUpperCase()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "File upload failed: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/enrollment/status/{enrollmentId}
    // Applicant can check their enrollment status
    // ────────────────────────────────────────────────────────────────────────
    @GetMapping("/status/{enrollmentId}")
    public ResponseEntity<?> checkStatus(@PathVariable String enrollmentId) {
        Optional<Enrollment> enrollment = enrollmentService.getByEnrollmentId(enrollmentId);
        if (enrollment.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Enrollment ID not found"));
        }
        Enrollment e = enrollment.get();
        return ResponseEntity.ok(Map.of(
                "enrollmentId", e.getEnrollmentId(),
                "status", e.getStatus(),
                "fullNameEn", e.getFullNameEn() != null ? e.getFullNameEn() : "",
                "createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : "",
                "reviewNotes", e.getReviewNotes() != null ? e.getReviewNotes() : ""
        ));
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/enrollment/all  (Admin only)
    // ────────────────────────────────────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }

    // ────────────────────────────────────────────────────────────────────────
    // PUT /api/enrollment/{enrollmentId}/review  (Admin only)
    // Update enrollment status
    // Body: { "status": "APPROVED", "reviewedBy": "admin", "notes": "..." }
    // ────────────────────────────────────────────────────────────────────────
    @PutMapping("/{enrollmentId}/review")
    public ResponseEntity<?> reviewEnrollment(
            @PathVariable String enrollmentId,
            @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            String reviewedBy = payload.get("reviewedBy");
            String notes = payload.getOrDefault("notes", "");

            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }

            Enrollment updated = enrollmentService.updateStatus(enrollmentId, status, reviewedBy, notes);
            return ResponseEntity.ok(Map.of(
                    "message", "Enrollment status updated",
                    "enrollmentId", updated.getEnrollmentId(),
                    "status", updated.getStatus()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // GET /api/enrollment/check-mobile?mobile=XXXXXXXXXX
    // ────────────────────────────────────────────────────────────────────────
    @GetMapping("/check-mobile")
    public ResponseEntity<?> checkMobile(@RequestParam String mobile) {
        boolean exists = enrollmentService.mobileAlreadyEnrolled(mobile);
        return ResponseEntity.ok(Map.of("enrolled", exists));
    }
}