package com.hcl.systembackend.controller;

import com.hcl.systembackend.dto.AnomalySummary;
import com.hcl.systembackend.dto.AdminTransactionHistoryItem;
import com.hcl.systembackend.dto.InvestigationQueueStatusUpdateRequest;
import com.hcl.systembackend.dto.InvestigationQueueItemView;
import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.service.AdminAuthService;
import com.hcl.systembackend.service.AdminTransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/investigation-queue")
    public ResponseEntity<List<InvestigationQueueItemView>> getInvestigationQueue(HttpServletRequest request) {
        adminAuthService.requireAdminId(request);
        return ResponseEntity.ok(adminTransactionService.getInvestigationQueueItems());
    }

    @GetMapping("/anomalies/summary")
    public ResponseEntity<AnomalySummary> getAnomalySummary(HttpServletRequest request) {
        adminAuthService.requireAdminId(request);
        return ResponseEntity.ok(adminTransactionService.getAnomalySummary());
    }

    @PatchMapping("/investigation-queue/{queueId}/status")
    public ResponseEntity<InvestigationQueueItemView> updateInvestigationQueueStatus(
            HttpServletRequest request,
            @PathVariable Long queueId,
            @RequestBody InvestigationQueueStatusUpdateRequest updateRequest
    ) {
        Integer adminId = adminAuthService.requireAdminId(request);
        return ResponseEntity.ok(adminTransactionService.updateInvestigationQueueStatus(queueId, updateRequest, adminId));
    }
}

