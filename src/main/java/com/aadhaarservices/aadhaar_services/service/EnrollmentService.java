package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.Enrollment;
import com.aadhaarservices.aadhaar_services.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // ─── Upload directory (same pattern as profile photos) ───────────────────
    private static final String UPLOAD_DIR = "uploads/enrollment/";

    // ─── Create new enrollment ───────────────────────────────────────────────
    public Enrollment createEnrollment(Enrollment enrollment) {
        // Generate unique EID
        String eid = generateEnrollmentId();
        enrollment.setEnrollmentId(eid);
        enrollment.setStatus("PENDING");
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    // ─── Upload a document for an enrollment ─────────────────────────────────
    public String uploadDocument(String enrollmentId, String docType, MultipartFile file)
            throws IOException {

        Enrollment enrollment = enrollmentRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        // Ensure folder exists
        String folderPath = UPLOAD_DIR + enrollmentId + "/";
        File folder = new File(folderPath);
        if (!folder.exists()) folder.mkdirs();

        // Unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = docType + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path filePath = Paths.get(folderPath + fileName);
        Files.write(filePath, file.getBytes());

        // Save path to enrollment
        String savedPath = "/uploads/enrollment/" + enrollmentId + "/" + fileName;
        switch (docType.toUpperCase()) {
            case "POI"   -> enrollment.setPoiDocumentPath(savedPath);
            case "POA"   -> enrollment.setPoaDocumentPath(savedPath);
            case "DOB"   -> enrollment.setDobDocumentPath(savedPath);
            case "PHOTO" -> enrollment.setPhotoPath(savedPath);
        }
        enrollment.setUpdatedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        return savedPath;
    }

    // ─── Get enrollment by EID ───────────────────────────────────────────────
    public Optional<Enrollment> getByEnrollmentId(String enrollmentId) {
        return enrollmentRepository.findByEnrollmentId(enrollmentId);
    }

    // ─── Get all enrollments (admin) ─────────────────────────────────────────
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    // ─── Get enrollments by status ───────────────────────────────────────────
    public List<Enrollment> getByStatus(String status) {
        return enrollmentRepository.findAllByStatus(status);
    }

    // ─── Update status (admin review) ────────────────────────────────────────
    public Enrollment updateStatus(String enrollmentId, String status,
                                   String reviewedBy, String notes) {
        Enrollment enrollment = enrollmentRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setStatus(status);
        enrollment.setReviewedBy(reviewedBy);
        enrollment.setReviewNotes(notes);
        enrollment.setReviewedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    // ─── Check if mobile already enrolled ────────────────────────────────────
    public boolean mobileAlreadyEnrolled(String mobile) {
        return enrollmentRepository.existsByMobileNumber(mobile);
    }

    // ─── Generate EID ────────────────────────────────────────────────────────
    private String generateEnrollmentId() {
        String year = DateTimeFormatter.ofPattern("yyyy").format(LocalDateTime.now());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "EID-" + year + "-" + random;
    }
}