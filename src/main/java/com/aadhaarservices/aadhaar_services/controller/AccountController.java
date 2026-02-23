package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.Account;
import com.aadhaarservices.aadhaar_services.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC — No auth required
    // Called by WalletPayment.jsx on page load.
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/bank-details")
    public ResponseEntity<?> getBankDetails() {
        try {
            Account account = accountService.getAccountInfo(null);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("accountHolderName", safe(account.getAccountHolderName()));
            response.put("accountNumber",     safe(account.getAccountNumber()));
            response.put("bankName",          safe(account.getBankName()));
            response.put("ifscCode",          safe(account.getIfscCode()));
            response.put("qrCodeImage",       safe(account.getQrCodeImage()));
            response.put("hasBankDetails",    hasBankDetails(account));
            response.put("hasQrCode",         account.getQrCodeImage() != null
                                              && !account.getQrCodeImage().isEmpty());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch bank details", "details", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN — Get full account by ID (for admin form load)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAccountById(@PathVariable Long id) {
        try {
            Account account = accountService.getAccountInfo(id);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch account", "details", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN — Update bank text details
    // PUT /api/account/{id}
    // Body (JSON): { accountNumber, accountHolderName, ifscCode, bankName, totalAmount }
    // ─────────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateBankDetails(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String accountNumber     = body.get("accountNumber");
            String accountHolderName = body.get("accountHolderName");
            String ifscCode          = body.get("ifscCode");
            String bankName          = body.get("bankName");
            String totalAmountStr    = body.get("totalAmount");

            if (accountNumber == null || accountHolderName == null ||
                ifscCode == null || bankName == null || totalAmountStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "All fields are required"));
            }

            BigDecimal totalAmount = new BigDecimal(totalAmountStr);
            Account updated = accountService.updateAccountInfo(
                    id, accountNumber, accountHolderName, ifscCode, bankName, totalAmount);

            return ResponseEntity.ok(Map.of(
                    "message",   "Bank details updated successfully",
                    "accountId", updated.getId()
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid totalAmount — must be a number"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to update bank details", "details", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN — Clear ALL bank details + QR (reset to blank)
    // DELETE /api/account/{id}
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearAllDetails(@PathVariable Long id) {
        try {
            Account cleared = accountService.clearAccountDetails(id);
            return ResponseEntity.ok(Map.of(
                    "message",   "All bank details and QR cleared. Reset to blank.",
                    "accountId", cleared.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to clear account details", "details", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN — Upload / Replace QR code
    // POST /api/account/{id}/qr  (multipart/form-data, field = "file")
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadQrCode(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No file provided"));
            }

            String mimeType = file.getContentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Only image files allowed (PNG, JPG, SVG, etc.)"));
            }

            if (file.getSize() > 3 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File too large. Maximum size is 3MB."));
            }

            Account account = accountService.getAccountInfo(id);

            String base64  = Base64.getEncoder().encodeToString(file.getBytes());
            String dataUri = "data:" + mimeType + ";base64," + base64;

            account.setQrCodeImage(dataUri);
            accountService.save(account);

            return ResponseEntity.ok(Map.of(
                    "message",   "QR uploaded successfully. Old QR replaced.",
                    "accountId", account.getId(),
                    "sizeKB",    file.getSize() / 1024
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to upload QR", "details", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN — Delete QR only (bank text details stay intact)
    // DELETE /api/account/{id}/qr
    // ─────────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}/qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteQrCode(@PathVariable Long id) {
        try {
            Account account = accountService.getAccountInfo(id);
            account.setQrCodeImage(null);
            accountService.save(account);
            return ResponseEntity.ok(Map.of(
                    "message", "QR code removed. Bank details still intact."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to remove QR", "details", e.getMessage()));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private String safe(String val) {
        return val != null ? val : "";
    }

    private boolean hasBankDetails(Account account) {
        return account.getAccountNumber()     != null && !account.getAccountNumber().isEmpty() &&
               account.getAccountHolderName() != null && !account.getAccountHolderName().isEmpty() &&
               account.getBankName()          != null && !account.getBankName().isEmpty() &&
               account.getIfscCode()          != null && !account.getIfscCode().isEmpty();
    }
}