package com.aadhaarservices.aadhaar_services.repository;

import com.aadhaarservices.aadhaar_services.model.MachineTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MachineTrackingRepo extends JpaRepository<MachineTracking, Long> {

    List<MachineTracking> findByApplicationId(Long applicationId);
}
