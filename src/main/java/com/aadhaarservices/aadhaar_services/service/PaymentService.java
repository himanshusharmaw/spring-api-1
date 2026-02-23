package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.Account;
import com.aadhaarservices.aadhaar_services.model.Payment;
import com.aadhaarservices.aadhaar_services.model.PaymentDetails;
import com.aadhaarservices.aadhaar_services.repository.AccountRepository;
import com.aadhaarservices.aadhaar_services.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Add a payment to a specific account.
     */
    public Payment addPaymentToAccount(Long accountId, BigDecimal amount) {
        // FIXED: findFirstById removed — use findById().orElse(null)
        Account account = accountRepository.findById(accountId).orElse(null);

        if (account != null) {
            Payment payment = new Payment();
            payment.setAccount(account);
            payment.setAmount(amount);
            return paymentRepository.save(payment);
        }
        return null;
    }

    /**
     * Retrieve the total amount paid for a specific account.
     */
    public BigDecimal getTotalPaymentsForAccount(Long accountId) {
        // FIXED: findFirstById removed — use findById().orElse(null)
        Account account = accountRepository.findById(accountId).orElse(null);

        if (account != null) {
            return paymentRepository.findAll().stream()
                .filter(payment -> payment.getAccount().getId().equals(accountId))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    public PaymentDetails verifyPayment(String userName, Double amount) {
        return null;
    }

    public String generateReceipt(PaymentDetails paymentDetails) {
        return null;
    }
}