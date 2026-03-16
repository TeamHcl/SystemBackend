package com.hcl.systembackend.dto;

import java.time.LocalDateTime;

public record AdminTransactionHistoryItem(
        Long transactionId,
        Long accountId,
        Integer customerId,
        Double amount,
        String transactionType,
        LocalDateTime transactionTime,
        Double similarityScore,
        Double anomalyScore,
        boolean flaggedFraud,
        String fraudReason
) {
}
