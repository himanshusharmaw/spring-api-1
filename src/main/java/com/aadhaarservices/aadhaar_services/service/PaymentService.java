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
     *
     * @param accountId The account ID to which the payment is linked.
     * @param amount    The payment amount.
     * @return The created payment object.
     */
    public Payment addPaymentToAccount(Long accountId, BigDecimal amount) {
        // Fetch the account based on accountId
        Account account = accountRepository.findFirstById(accountId);
        
        if (account != null) {
            // Create a new Payment object
            Payment payment = new Payment();
            payment.setAccount(account);
            payment.setAmount(amount);

            // Save the payment to the database
            return paymentRepository.save(payment);
        }

        // If account not found, return null (can throw exception depending on requirements)
        return null;
    }

    /**
     * Retrieve the total amount paid for a specific account.
     *
     * @param accountId The account ID to check the payments.
     * @return The total amount paid for that account.
     */
    public BigDecimal getTotalPaymentsForAccount(Long accountId) {
        // Fetch the account
        Account account = accountRepository.findFirstById(accountId);
        
        if (account != null) {
            // Sum all payments linked to the account
            return paymentRepository.findAll().stream()
                .filter(payment -> payment.getAccount().getId().equals(accountId))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // If account not found, return zero
        return BigDecimal.ZERO;
    }

	public PaymentDetails verifyPayment(String userName, Double amount) {
		// TODO Auto-generated method stub
		return null;
	}

	public String generateReceipt(PaymentDetails paymentDetails) {
		// TODO Auto-generated method stub
		return null;
	}
}
