package com.aadhaarservices.aadhaar_services.payload;

import lombok.Data;

@Data
public class ProfileResponse {
    private String fullName;
    private String profilePhoto;
    private String aadhaarNumber;
    private String email;
    private String phone;
    private String address;
    private String registrationDate;
    private double walletBalance;

    private boolean twoFA;
    private String lastLogin;

    private boolean aadhaarVerified;
    private boolean panVerified;
}
