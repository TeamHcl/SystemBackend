package com.hcl.systembackend.dto;

import java.time.LocalDateTime;

public record InvestigationQueueItemView(
        Long queueId,
        Long transactionId,
        Integer customerId,
        Integer riskScore,
        String reason,
        String status,
        LocalDateTime createdAt
) {
}
