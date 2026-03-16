package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.AdminTransactionHistoryItem;
import com.hcl.systembackend.dto.TransactionRiskMetrics;
import com.hcl.systembackend.entity.Account;
import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.repository.AccountRepository;
import com.hcl.systembackend.repository.TransactionRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminTransactionService {
    private static final String PGVECTOR_HISTORY_SQL = """
            WITH feature_rows AS (
                SELECT
                    t.transaction_id AS id,
                    t.account_id,
                    a.customer_id,
                    t.amount,
                    t.transaction_type,
                    t.transaction_time,
                    CASE t.transaction_type
                        WHEN 'DEPOSIT' THEN 0.0
                        WHEN 'WITHDRAWAL' THEN 1.0
                        WHEN 'TRANSFER' THEN 2.0
                        WHEN 'PAYMENT' THEN 3.0
                        ELSE 4.0
                    END AS type_bucket,
                    EXTRACT(HOUR FROM t.transaction_time) / 23.0 AS hour_bucket,
                    COALESCE(
                        EXTRACT(EPOCH FROM (
                            t.transaction_time - LAG(t.transaction_time) OVER (
                                PARTITION BY COALESCE(a.customer_id, -1)
                                ORDER BY t.transaction_time, t.transaction_id
                            )
                        )) / 86400.0,
                        0.0
                    ) AS gap_days
                FROM transactions t
                LEFT JOIN accounts a ON a.account_id = t.account_id
            ),
            feature_vectors AS (
                SELECT
                    fr.*,
                    CONCAT(
                        '[',
                        ROUND(CAST(COALESCE(fr.amount, 0.0) AS numeric), 4), ',',
                        ROUND(CAST(fr.type_bucket AS numeric), 4), ',',
                        ROUND(CAST(fr.hour_bucket AS numeric), 4), ',',
                        ROUND(CAST(LEAST(fr.gap_days, 30.0) AS numeric), 4),
                        ']'
                    )::vector AS feature_vector
                FROM feature_rows fr
            ),
            ranked AS (
                SELECT
                    fv.*,
                    AVG(fv.feature_vector) OVER () AS global_profile,
                    AVG(fv.feature_vector) OVER (
                        PARTITION BY COALESCE(fv.customer_id, -1)
                        ORDER BY fv.transaction_time, fv.id
                        ROWS BETWEEN 5 PRECEDING AND 1 PRECEDING
                    ) AS customer_profile,
                    AVG(fv.amount) OVER (
                        PARTITION BY COALESCE(fv.customer_id, -1)
                        ORDER BY fv.transaction_time, fv.id
                        ROWS BETWEEN 5 PRECEDING AND 1 PRECEDING
                    ) AS recent_avg_amount
                FROM feature_vectors fv
            ),
            scored AS (
                SELECT
                    r.*,
                    COALESCE(r.customer_profile, r.global_profile) AS baseline_vector,
                    1 - (r.feature_vector <=> COALESCE(r.customer_profile, r.global_profile)) AS similarity_score,
                    CASE
                        WHEN r.recent_avg_amount IS NULL OR r.recent_avg_amount = 0 THEN 1.0
                        ELSE r.amount / r.recent_avg_amount
                    END AS amount_multiplier
                FROM ranked r
            )
            SELECT
                s.id,
                s.account_id,
                s.customer_id,
                s.amount,
                s.transaction_type,
                s.transaction_time,
                ROUND(CAST(COALESCE(s.similarity_score, 1.0) AS numeric), 4) AS similarity_score,
                ROUND(CAST((1 - COALESCE(s.similarity_score, 1.0)) * 100 AS numeric), 2) AS anomaly_score,
                CASE
                    WHEN COALESCE(s.similarity_score, 1.0) < 0.82 THEN true
                    WHEN s.amount_multiplier >= 2.50 THEN true
                    ELSE false
                END AS flagged_fraud,
                CASE
                    WHEN COALESCE(s.similarity_score, 1.0) < 0.82 AND s.amount_multiplier >= 2.50
                        THEN 'transaction deviates from the customer profile and amount is unusually high'
                    WHEN COALESCE(s.similarity_score, 1.0) < 0.82
                        THEN 'transaction pattern is dissimilar to the customer''s recent behavior'
                    WHEN s.amount_multiplier >= 2.50
                        THEN 'transaction amount is unusually high for this customer'
                    ELSE 'transaction matches the customer profile'
                END AS fraud_reason
            FROM scored s
            ORDER BY s.transaction_time DESC, s.id DESC
            """;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Autowired
    public AdminTransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public List<AdminTransactionHistoryItem> getAllTransactions() {
        if (isPostgreSql()) {
            try {
                return getTransactionsWithPgvector();
            } catch (DataAccessException ex) {
                return getTransactionsWithFallbackScoring();
            }
        }
        return getTransactionsWithFallbackScoring();
    }

    public List<Transaction> getRawTransactions() {
        return transactionRepository.findAllByOrderByTransactionTimeDesc();
    }

    private boolean isPostgreSql() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
        } catch (SQLException ex) {
            return false;
        }
    }

    private List<AdminTransactionHistoryItem> getTransactionsWithPgvector() {
        return jdbcTemplate.query(
                PGVECTOR_HISTORY_SQL,
                (rs, rowNum) -> new AdminTransactionHistoryItem(
                        rs.getLong("id"),
                        rs.getLong("account_id"),
                        getNullableInteger(rs.getObject("customer_id")),
                        rs.getDouble("amount"),
                        rs.getString("transaction_type"),
                        rs.getTimestamp("transaction_time").toLocalDateTime(),
                        rs.getDouble("similarity_score"),
                        rs.getDouble("anomaly_score"),
                        rs.getBoolean("flagged_fraud"),
                        rs.getString("fraud_reason")
                )
        );
    }

    private List<AdminTransactionHistoryItem> getTransactionsWithFallbackScoring() {
        Map<Long, Integer> customerByAccountId = accountRepository.findAll().stream()
                .collect(Collectors.toMap(Account::getAccountId, Account::getCustomerId, (left, right) -> left));
        Map<Integer, List<Transaction>> transactionsByCustomer = new HashMap<>();
        List<Transaction> transactions = transactionRepository.findAllByOrderByTransactionTimeDesc().stream()
                .sorted(Comparator.comparing(Transaction::getTransactionTime))
                .toList();

        return transactions.stream()
                .map(transaction -> toHistoryItem(transaction, customerByAccountId, transactionsByCustomer))
                .sorted(Comparator.comparing(AdminTransactionHistoryItem::transactionTime).reversed())
                .toList();
    }

    private AdminTransactionHistoryItem toHistoryItem(
            Transaction transaction,
            Map<Long, Integer> customerByAccountId,
            Map<Integer, List<Transaction>> transactionsByCustomer
    ) {
        Integer customerId = customerByAccountId.get(transaction.getAccountId());
        List<Transaction> history = customerId == null
                ? List.of()
                : transactionsByCustomer.getOrDefault(customerId, List.of());
        TransactionRiskMetrics metrics = calculateFallbackMetrics(transaction, history);

        if (customerId != null) {
            transactionsByCustomer.computeIfAbsent(customerId, ignored -> new java.util.ArrayList<>()).add(transaction);
        }

        return new AdminTransactionHistoryItem(
                transaction.getId(),
                transaction.getAccountId(),
                customerId,
                transaction.getAmount(),
                transaction.getTransactionType().name(),
                transaction.getTransactionTime(),
                metrics.similarityScore(),
                metrics.anomalyScore(),
                metrics.flaggedFraud(),
                metrics.fraudReason()
        );
    }

    private TransactionRiskMetrics calculateFallbackMetrics(Transaction transaction, List<Transaction> history) {
        if (history.isEmpty()) {
            return new TransactionRiskMetrics(1.0, 0.0, false, "no prior customer history available");
        }

        double averageAmount = history.stream()
                .mapToDouble(Transaction::getAmount)
                .average()
                .orElse(transaction.getAmount());
        long matchingTypes = history.stream()
                .filter(item -> item.getTransactionType() == transaction.getTransactionType())
                .count();
        double typeMatchRatio = (double) matchingTypes / history.size();

        LocalDateTime previousTime = history.get(history.size() - 1).getTransactionTime();
        double gapDays = Math.max(0.0, Duration.between(previousTime, transaction.getTransactionTime()).toHours() / 24.0);
        double amountDeviation = averageAmount == 0.0
                ? 0.0
                : Math.abs(transaction.getAmount() - averageAmount) / averageAmount;
        double normalizedGap = Math.min(gapDays / 30.0, 1.0);

        double anomalyScore = Math.min(100.0, ((amountDeviation * 0.55) + ((1 - typeMatchRatio) * 0.30) + (normalizedGap * 0.15)) * 100.0);
        double similarityScore = Math.max(0.0, 1.0 - (anomalyScore / 100.0));
        boolean flagged = similarityScore < 0.82 || transaction.getAmount() >= averageAmount * 2.5;

        String reason = flagged
                ? "fallback fraud score flagged this transaction as abnormal for the customer"
                : "fallback fraud score considered this transaction normal";

        return new TransactionRiskMetrics(similarityScore, anomalyScore, flagged, reason);
    }

    private Integer getNullableInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }
}

