package com.hcl.systembackend.dto;

public record InvestigationQueueStatusUpdateRequest(
        String status,
        String note
) {
}
