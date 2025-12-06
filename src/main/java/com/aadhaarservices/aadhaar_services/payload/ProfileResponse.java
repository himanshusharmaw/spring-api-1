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

    private double walletBalance;
    private String walletStatus;
    private String registrationDate;

    private boolean twoFA;
    private String lastLogin;

    private boolean aadhaarVerified;
    private boolean panVerified;

    private Boolean emailVerified;
    private Boolean mobileVerified;
    private Boolean aadhaarLinked;
    private Boolean accountLocked;

    private String recentActivity;
    private String dateOfBirth;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String branch;

}
