package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.Account;
import com.aadhaarservices.aadhaar_services.model.Payment;
import com.aadhaarservices.aadhaar_services.repository.AccountRepository;
import com.aadhaarservices.aadhaar_services.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public Account getAccountInfo(Long id) {
        return accountRepository.findFirstById(id);  // Get the first account (assuming only one account exists)
    }

    public Account updateAccountInfo(Long id, String accountNumber, String accountHolderName, String ifscCode, String bankName, BigDecimal totalAmount) {
        Account account = accountRepository.findFirstById(id);
        if (account != null) {
            account.setAccountNumber(accountNumber);
            account.setAccountHolderName(accountHolderName);
            account.setIfscCode(ifscCode);
            account.setBankName(bankName);
            account.setTotalAmount(totalAmount);
            return accountRepository.save(account);
        }
        return null;
    }
}
