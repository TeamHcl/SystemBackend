package com.hcl.systembackend.dto;

import java.time.LocalDateTime;

public record MockBankHistorySyncResult(
        int usersSynced,
        int accountsSynced,
        int transactionsInserted,
        int transactionsUpdated,
        int transactionsSkipped,
        int pagesFetched,
        LocalDateTime syncedAt,
        String message
) {
}
