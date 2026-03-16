package com.hcl.systembackend.controller;

import com.hcl.systembackend.dto.AdminTransactionHistoryItem;
import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.service.AdminAuthService;
import com.hcl.systembackend.service.AdminTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminTransactionController {
    private final AdminTransactionService adminTransactionService;
    private final AdminAuthService adminAuthService;

    @Autowired
    public AdminTransactionController(AdminTransactionService adminTransactionService, AdminAuthService adminAuthService) {
        this.adminTransactionService = adminTransactionService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<AdminTransactionHistoryItem>> getAllTransactions(HttpServletRequest request) {
        adminAuthService.requireAdminId(request);
        List<AdminTransactionHistoryItem> transactions = adminTransactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions-raw")
    public ResponseEntity<List<Transaction>> getRawTransactions(HttpServletRequest request) {
        adminAuthService.requireAdminId(request);
        List<Transaction> transactions = adminTransactionService.getRawTransactions();
        return ResponseEntity.ok(transactions);
    }
}

