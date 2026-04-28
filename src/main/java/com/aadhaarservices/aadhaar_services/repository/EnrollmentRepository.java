package com.aadhaarservices.aadhaar_services.repository;

import com.aadhaarservices.aadhaar_services.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByEnrollmentId(String enrollmentId);

    List<Enrollment> findAllByStatus(String status);

    boolean existsByMobileNumber(String mobileNumber);
}