package com.aadhaarservices.aadhaar_services.repository;

import com.aadhaarservices.aadhaar_services.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // Use Optional so we can safely check existence without null checks
    Optional<Account> findById(Long id);
}