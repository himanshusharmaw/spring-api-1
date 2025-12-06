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
    public Wallet addFunds(User user, BigDecimal amount) {

        Wallet wallet = user.getWallet();

        wallet.setBalance(wallet.getBalance().add(amount));

        // automatically activate wallet on first transaction
        if (wallet.getStatus() == null || wallet.getStatus().equals("INACTIVE")) {
            wallet.setStatus("ACTIVE");
        }

        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet deductFunds(User user, BigDecimal amount) {

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        BigDecimal current = wallet.getBalance();

        if (current.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        wallet.setBalance(current.subtract(amount));

        return walletRepository.save(wallet);
    }
}
