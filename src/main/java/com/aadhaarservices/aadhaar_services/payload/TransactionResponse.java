package com.aadhaarservices.aadhaar_services.payload;


import lombok.Data;

@Data
public class TransactionResponse {
    private Long id;
    private double amount;
    private String type;
    private String status;
    private String createdAt;
    private String referenceId;
    private String description;
}
