// Path: repository/UserRepository.java
package com.aadhaarservices.aadhaar_services.repository;

import com.aadhaarservices.aadhaar_services.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}