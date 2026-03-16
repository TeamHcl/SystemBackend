package com.hcl.systembackend.controller;

import com.hcl.systembackend.dto.AuthResponse;
import com.hcl.systembackend.dto.SigninRequest;
import com.hcl.systembackend.service.AdminAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminAuthService adminAuthService;

    public AuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody SigninRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }
}
