package com.hcl.systembackend.controller;

import com.hcl.systembackend.dto.AdminUserView;
import com.hcl.systembackend.dto.SignupRequest;
import com.hcl.systembackend.service.AdminAuthService;
import com.hcl.systembackend.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuthService adminAuthService;
    private final AdminUserService adminUserService;

    public AdminController(AdminAuthService adminAuthService, AdminUserService adminUserService) {
        this.adminAuthService = adminAuthService;
        this.adminUserService = adminUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(adminAuthService.registerAdmin(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserView>> getAllUsers(HttpServletRequest request) {
        adminAuthService.requireAdminId(request);
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }
}
