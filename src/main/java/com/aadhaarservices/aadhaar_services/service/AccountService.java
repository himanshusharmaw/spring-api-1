package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.Account;
import com.aadhaarservices.aadhaar_services.repository.AccountRepository;
import com.aadhaarservices.aadhaar_services.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // ─────────────────────────────────────────────────────────────────
    // Get account — always returns one.
    //
    // Strategy:
    // 1. Try to find by the requested ID first.
    // 2. If not found, check if ANY account exists (use the first one).
    // 3. If no accounts at all, create a fresh blank one — DB assigns ID.
    //
    // This works correctly with @GeneratedValue because we never force
    // an ID — we let Postgres assign it, then reuse that record forever.
    // ─────────────────────────────────────────────────────────────────
    public Account getAccountInfo(Long id) {
        // Try exact ID first
        if (id != null) {
            var found = accountRepository.findById(id);
            if (found.isPresent()) {
                return found.get();
            }
        }

        // Fall back: use whichever account exists (first one)
        List<Account> all = accountRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }

        // No accounts at all — create a blank one, let DB assign ID
        Account account = new Account();
        account.setAccountNumber("");
        account.setAccountHolderName("");
        account.setIfscCode("");
        account.setBankName("");
        account.setTotalAmount(BigDecimal.ZERO);
        account.setQrCodeImage(null);
        return accountRepository.save(account);
    }

    // ─────────────────────────────────────────────────────────────────
    // Update bank text details
    // ─────────────────────────────────────────────────────────────────
    public Account updateAccountInfo(Long id, String accountNumber, String accountHolderName,
                                     String ifscCode, String bankName, BigDecimal totalAmount) {
        Account account = getAccountInfo(id);
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(accountHolderName);
        account.setIfscCode(ifscCode);
        account.setBankName(bankName);
        account.setTotalAmount(totalAmount);
        return accountRepository.save(account);
    }

    // ─────────────────────────────────────────────────────────────────
    // Save — used by controller for QR upload/delete
    // ─────────────────────────────────────────────────────────────────
    public Account save(Account account) {
        return accountRepository.save(account);
    }

    // ─────────────────────────────────────────────────────────────────
    // Clear ALL fields — resets to blank, keeps the DB row alive
    // ─────────────────────────────────────────────────────────────────
    public Account clearAccountDetails(Long id) {
        Account account = getAccountInfo(id);
        account.setAccountNumber("");
        account.setAccountHolderName("");
        account.setIfscCode("");
        account.setBankName("");
        account.setTotalAmount(BigDecimal.ZERO);
        account.setQrCodeImage(null);
        return accountRepository.save(account);
    }
}