package com.aadhaarservices.aadhaar_services.controller;

import com.aadhaarservices.aadhaar_services.model.PaymentDetails;
import com.aadhaarservices.aadhaar_services.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/verify")
    public PaymentDetails verifyPayment(@RequestParam String userName, @RequestParam Double amount) {
        return paymentService.verifyPayment(userName, amount);
    }

    @PostMapping("/receipt")
    public String generateReceipt(@RequestBody PaymentDetails paymentDetails) {
        return paymentService.generateReceipt(paymentDetails);
    }
}