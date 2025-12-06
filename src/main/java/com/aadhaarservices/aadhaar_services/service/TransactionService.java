package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.Transaction;
import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.payload.TransactionResponse;
import com.aadhaarservices.aadhaar_services.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepo;

    @Autowired
    private NotificationService notificationService;

    public Transaction record(User user, BigDecimal amount, String type, String status, String description) {

        Transaction txn = new Transaction();
        txn.setUser(user);
        txn.setAmount(amount);
        txn.setType(type);
        txn.setStatus(status);
        txn.setDescription(description);
        txn.setReferenceId("TXN-" + System.currentTimeMillis());

        Transaction saved = transactionRepo.save(txn);

        notificationService.create(
                user.getId(),
                "Transaction: " + type,
                description
        );

        return saved;
    }

    // Get all transactions for a single user
    public List<Transaction> getTransactions(User user) {
        return transactionRepo.findByUserOrderByCreatedAtDesc(user);
    }

    // For /api/wallet/transactions?limit=5
    public List<TransactionResponse> getUserTransactions(Long userId, int limit) {

        Pageable pageable = PageRequest.of(0, limit);

        List<Transaction> txns =
                transactionRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return txns.stream().map(tx -> {
            TransactionResponse dto = new TransactionResponse();
            dto.setId(tx.getId());
            dto.setAmount(tx.getAmount().doubleValue());
            dto.setType(tx.getType());
            dto.setStatus(tx.getStatus());
            dto.setCreatedAt(tx.getCreatedAt().toString());
            dto.setReferenceId(tx.getReferenceId());
            dto.setDescription(tx.getDescription());
            return dto;
        }).collect(Collectors.toList());
    }

    // Admin manually updates status for withdrawals
    public Transaction updateStatus(Long id, String newStatus) {
        Transaction txn = transactionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        txn.setStatus(newStatus);
        return transactionRepo.save(txn);
    }
}
