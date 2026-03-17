package com.hcl.systembackend.dto;

public record AnomalySummary(
        int totalTransactions,
        int flaggedTransactions,
        int flaggedUsers,
        long openQueueItems
) {
}
