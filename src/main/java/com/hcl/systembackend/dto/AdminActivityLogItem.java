package com.hcl.systembackend.dto;

import java.time.LocalDateTime;

public record AdminActivityLogItem(
        Long id,
        Integer adminId,
        String action,
        String details,
        LocalDateTime createdAt
) {
}
