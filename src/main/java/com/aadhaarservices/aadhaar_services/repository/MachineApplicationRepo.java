package com.aadhaarservices.aadhaar_services.repository;

import com.aadhaarservices.aadhaar_services.model.MachineApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MachineApplicationRepo extends JpaRepository<MachineApplication, Long> {

    long countByUserId(Long userId);

    List<MachineApplication> findByUserId(Long userId);

    MachineApplication findTopByUserIdOrderByIdDesc(Long userId); // Latest application
}
