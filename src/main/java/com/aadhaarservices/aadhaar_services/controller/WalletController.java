package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.Transaction;
import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.model.Wallet;
import com.aadhaarservices.aadhaar_services.payload.TransactionResponse;
import com.aadhaarservices.aadhaar_services.service.NotificationService;
import com.aadhaarservices.aadhaar_services.service.TransactionService;
import com.aadhaarservices.aadhaar_services.service.UserService;
import com.aadhaarservices.aadhaar_services.service.WalletService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    // -------------------------------------------------------
    // ADD FUNDS
    // -------------------------------------------------------
    @PostMapping("/add-funds")
    public ResponseEntity<?> addFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam BigDecimal amount) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User user = userService.getUserByUsername(userDetails.getUsername());

            Wallet wallet = walletService.addFunds(user, amount);

            // Record transaction
            transactionService.record(
                    user,
                    amount,
                    "ADD_FUNDS",
                    "SUCCESS",
                    "₹" + amount + " added to wallet."
            );

            // Send notification
            notificationService.create(
                    user.getId(),
                    "Funds Added",
                    "₹" + amount + " has been added to your wallet."
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Funds added successfully",
                            "balance", wallet.getBalance(),
                            "status", wallet.getStatus()
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Failed to add funds", "details", e.getMessage())
            );
        }
    }

    // -------------------------------------------------------
    // DEDUCT FUNDS
    // -------------------------------------------------------
    @PostMapping("/deduct-funds")
    public ResponseEntity<?> deductFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam BigDecimal amount) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User user = userService.getUserByUsername(userDetails.getUsername());
            Wallet wallet = walletService.deductFunds(user, amount);

            // Record transaction
            transactionService.record(
                    user,
                    amount,
                    "DEDUCT_FUNDS",
                    "SUCCESS",
                    "₹" + amount + " deducted from wallet."
            );

            // Send notification
            notificationService.create(
                    user.getId(),
                    "Funds Deducted",
                    "₹" + amount + " has been deducted from your wallet."
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Funds deducted successfully",
                            "balance", wallet.getBalance(),
                            "status", wallet.getStatus()
                    ));

        } catch (IllegalArgumentException ex) {
            return handleFailedTransaction(userDetails, amount, "DEDUCT_FUNDS", ex.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Failed to deduct funds", "details", e.getMessage())
            );
        }
    }

    // -------------------------------------------------------
    // WITHDRAW FUNDS
    // -------------------------------------------------------
    @PostMapping("/withdraw")
    public ResponseEntity<?> requestWithdraw(
            @AuthenticationPrincipal User user,
            @RequestParam BigDecimal amount) {

        if (user.getWallet().getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient balance"));
        }

        // Record withdrawal transaction as PENDING only
        Transaction txn = transactionService.record(
                user,
                amount,
                "WITHDRAW",
                "PENDING",
                "Withdrawal request submitted. Awaiting approval."
        );

        notificationService.create(
                user.getId(),
                "Withdrawal Requested",
                "₹" + amount + " withdrawal request submitted. Awaiting approval."
        );

        return ResponseEntity.ok(txn);
    }
    
    // -------------------------------------------------------
    // HANDLE FAILED TRANSACTIONS
    // -------------------------------------------------------
    private ResponseEntity<?> handleFailedTransaction(UserDetails userDetails, BigDecimal amount, String type, String message) {
        try {
            User user = userService.getUserByUsername(userDetails.getUsername());

            // Record failed transaction
            transactionService.record(
                    user,
                    amount,
                    type,
                    "FAILED",
                    message
            );

            // Send notification
            notificationService.create(
                    user.getId(),
                    "Transaction Failed",
                    message
            );

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", message,
                            "balance", user.getWallet().getBalance(),
                            "status", user.getWallet().getStatus()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Transaction failed and could not be recorded", "details", e.getMessage())
            );
        }
    }
    
 // -------------------------------------------------------
 // GET TRANSACTION HISTORY (with optional ?limit=5)
 // -------------------------------------------------------
 @GetMapping("/transactions")
 public ResponseEntity<?> getTransactions(
         @AuthenticationPrincipal UserDetails userDetails,
         @RequestParam(required = false, defaultValue = "50") int limit) {

     if (userDetails == null) {
         return ResponseEntity.status(401).body("Unauthorized");
     }

     try {
         User user = userService.getUserByUsername(userDetails.getUsername());

         // Fetch transactions for user
         List<TransactionResponse> transactionDTOs =
        	        transactionService.getUserTransactions(user.getId(), limit);

        	return ResponseEntity.ok(transactionDTOs);


     } catch (Exception e) {
         return ResponseEntity.status(500).body(
                 Map.of("error", "Failed to fetch transactions", "details", e.getMessage())
         );
     }
 }

}
