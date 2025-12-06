package com.aadhaarservices.aadhaar_services.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aadhaarservices.aadhaar_services.model.User;
import com.aadhaarservices.aadhaar_services.service.MachineService;

@RestController
@RequestMapping("/api/machine")
public class MachineController {

    @Autowired
    private MachineService machineService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyMachine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(machineService.apply(user.getId()));
    }

    @GetMapping("/user-machines")
    public ResponseEntity<?> getUserMachines(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(machineService.getUserMachines(user.getId()));
    }

    @GetMapping("/tracking")
    public ResponseEntity<?> getTracking(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(machineService.getTracking(user.getId()));
    }
}
