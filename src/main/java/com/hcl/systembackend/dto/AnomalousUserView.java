package com.hcl.systembackend.dto;

public record AnomalousUserView(
        Long customerId,
        String name,
        String email,
        int flaggedTransactionCount,
        double averageRiskScore,
        double maxRiskScore,
        String reason
) {
}
