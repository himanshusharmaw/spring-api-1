package com.aadhaarservices.aadhaar_services.service;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Wallet;
import com.aadhaarservices.aadhaar_services.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Transactional
    public void addFunds(User user, BigDecimal amount) {
        Wallet wallet = user.getWallet();
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }
    @Transactional
    public void deductFunds(User user, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal current = wallet.getBalance();
        if (current.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                "Current balance (" + current + ") is less than requested amount (" + amount + ")"
            );
        }
        wallet.setBalance(current.subtract(amount));
        walletRepository.save(wallet);
    }
}