package com.hcl.systembackend.controller;

import com.hcl.systembackend.dto.AnomalousUserView;
import com.hcl.systembackend.dto.AdminUserView;
import com.hcl.systembackend.dto.MockBankHistorySyncResult;
import com.hcl.systembackend.dto.SignupRequest;
import com.hcl.systembackend.service.AdminAuthService;
import com.hcl.systembackend.service.AdminUserService;
import com.hcl.systembackend.service.MockBankHistorySyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuthService adminAuthService;
    private final AdminUserService adminUserService;
    private final MockBankHistorySyncService mockBankHistorySyncService;

    public AdminController(
            AdminAuthService adminAuthService,
            AdminUserService adminUserService,
            MockBankHistorySyncService mockBankHistorySyncService
    ) {
        this.adminAuthService = adminAuthService;
        this.adminUserService = adminUserService;
        this.mockBankHistorySyncService = mockBankHistorySyncService;
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

    @GetMapping("/users/anomalies")
    public ResponseEntity<List<AnomalousUserView>> getAnomalousUsers(HttpServletRequest request) {
        adminAuthService.requireAdminId(request);
        return ResponseEntity.ok(adminUserService.getAnomalousUsers());
    }

    @PostMapping("/sync/mockbank-history")
    public ResponseEntity<MockBankHistorySyncResult> syncMockBankHistory(
            HttpServletRequest request,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since
    ) {
        adminAuthService.requireAdminId(request);
        return ResponseEntity.ok(mockBankHistorySyncService.syncHistory(since));
    }
}
