package com.hcl.systembackend.dto;

public record TransactionRiskMetrics(
        double similarityScore,
        double anomalyScore,
        boolean flaggedFraud,
        String fraudReason
) {
}
