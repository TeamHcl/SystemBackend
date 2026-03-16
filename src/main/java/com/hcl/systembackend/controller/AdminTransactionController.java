package com.hcl.systembackend.controller;

import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.service.AdminTransactionService;
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

    @Autowired
    public AdminTransactionController(AdminTransactionService adminTransactionService) {
        this.adminTransactionService = adminTransactionService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = adminTransactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions-raw")
    public ResponseEntity<List<Transaction>> getRawTransactions() {
        List<Transaction> transactions = adminTransactionService.getRawTransactions();
        return ResponseEntity.ok(transactions);
    }
}

