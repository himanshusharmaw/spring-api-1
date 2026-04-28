package com.aadhaarservices.aadhaar_services.repository;

import com.aadhaarservices.aadhaar_services.model.Transaction;
import com.aadhaarservices.aadhaar_services.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserOrderByCreatedAtDesc(User user);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // ← NEW: Admin – fetch all transactions of a given type (e.g. ADD_FUNDS)
    List<Transaction> findByTypeOrderByCreatedAtDesc(String type);

    // ← NEW: Admin – all transactions, newest first
    List<Transaction> findAllByOrderByCreatedAtDesc();
}