package com.aadhaarservices.aadhaar_services.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Tracking ─────────────────────────────────────────────────────────────
    @Column(unique = true, nullable = false)
    private String enrollmentId;           // EID like "EID-2024-XXXXXXXX"

    private String status;                 // PENDING | UNDER_REVIEW | APPROVED | REJECTED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Personal Information ──────────────────────────────────────────────────
    @Column(nullable = false)
    private String fullNameEn;             // Full Name in English

    private String fullNameHi;             // Full Name in Hindi (Devanagari)

    @Column(nullable = false)
    private String dateOfBirth;            // yyyy-MM-dd

    @Column(nullable = false)
    private String gender;                 // MALE | FEMALE | TRANSGENDER

    private String relationshipType;       // SELF | FATHER | MOTHER | SPOUSE | GUARDIAN
    private String relativeFullName;       // HOF / parent / guardian name

    @Column(nullable = false)
    private String mobileNumber;

    private String email;

    // ── Address ──────────────────────────────────────────────────────────────
    private String careOf;                 // C/O
    private String houseNo;
    private String street;
    private String landmark;
    private String area;
    private String village;                // Village / Town / City
    private String postOffice;
    private String district;
    private String subDistrict;
    private String state;
    private String pinCode;

    // ── Documents ────────────────────────────────────────────────────────────
    // Each stores the server-side file path after upload
    private String poiDocumentType;        // e.g. "Passport", "Voter ID", "Driving License"
    private String poiDocumentPath;        // path on disk

    private String poaDocumentType;        // Proof of Address type
    private String poaDocumentPath;

    private String dobDocumentType;        // Proof of DoB type
    private String dobDocumentPath;

    private String photoPath;              // Applicant recent photo

    // ── Admin / Review ───────────────────────────────────────────────────────
    private String reviewedBy;
    private String reviewNotes;
    private LocalDateTime reviewedAt;

    // ── Declaration ──────────────────────────────────────────────────────────
    private boolean declarationAccepted;

    // ─────────────────────────────────────────────────────────────────────────
    // Getters & Setters
    // ─────────────────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFullNameEn() { return fullNameEn; }
    public void setFullNameEn(String fullNameEn) { this.fullNameEn = fullNameEn; }

    public String getFullNameHi() { return fullNameHi; }
    public void setFullNameHi(String fullNameHi) { this.fullNameHi = fullNameHi; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getRelationshipType() { return relationshipType; }
    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public String getRelativeFullName() { return relativeFullName; }
    public void setRelativeFullName(String relativeFullName) { this.relativeFullName = relativeFullName; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCareOf() { return careOf; }
    public void setCareOf(String careOf) { this.careOf = careOf; }

    public String getHouseNo() { return houseNo; }
    public void setHouseNo(String houseNo) { this.houseNo = houseNo; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getPostOffice() { return postOffice; }
    public void setPostOffice(String postOffice) { this.postOffice = postOffice; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getSubDistrict() { return subDistrict; }
    public void setSubDistrict(String subDistrict) { this.subDistrict = subDistrict; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getPoiDocumentType() { return poiDocumentType; }
    public void setPoiDocumentType(String poiDocumentType) { this.poiDocumentType = poiDocumentType; }

    public String getPoiDocumentPath() { return poiDocumentPath; }
    public void setPoiDocumentPath(String poiDocumentPath) { this.poiDocumentPath = poiDocumentPath; }

    public String getPoaDocumentType() { return poaDocumentType; }
    public void setPoaDocumentType(String poaDocumentType) { this.poaDocumentType = poaDocumentType; }

    public String getPoaDocumentPath() { return poaDocumentPath; }
    public void setPoaDocumentPath(String poaDocumentPath) { this.poaDocumentPath = poaDocumentPath; }

    public String getDobDocumentType() { return dobDocumentType; }
    public void setDobDocumentType(String dobDocumentType) { this.dobDocumentType = dobDocumentType; }

    public String getDobDocumentPath() { return dobDocumentPath; }
    public void setDobDocumentPath(String dobDocumentPath) { this.dobDocumentPath = dobDocumentPath; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public boolean isDeclarationAccepted() { return declarationAccepted; }
    public void setDeclarationAccepted(boolean declarationAccepted) { this.declarationAccepted = declarationAccepted; }
}