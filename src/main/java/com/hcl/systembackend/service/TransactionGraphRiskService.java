package com.hcl.systembackend.service;

import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TransactionGraphRiskService {
    private final TransactionRepository transactionRepository;

    public TransactionGraphRiskService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Map<Long, GraphRiskMetrics> scoreByTransactionId() {
        List<Transaction> orderedTransactions = transactionRepository.findAllByOrderByTransactionTimeDesc().stream()
                .sorted(Comparator.comparing(Transaction::getTransactionTime).thenComparing(Transaction::getId))
                .toList();

        Map<Long, GraphRiskMetrics> scoreByTransactionId = new HashMap<>();

        for (int i = 0; i < orderedTransactions.size(); i++) {
            Transaction current = orderedTransactions.get(i);
            if (current.getId() == null || current.getTransactionTime() == null) {
                continue;
            }

            double score = 0.0;
            List<String> reasons = new ArrayList<>();

            if (isTransferLike(current) && current.getCounterpartyAccountId() != null) {
                long priorEdgeCount = countPriorEdges(orderedTransactions, i, current);
                if (priorEdgeCount == 0) {
                    score += 35.0;
                    reasons.add("first-time account-to-account edge");
                }

                long distinctCounterparties24h = countDistinctCounterpartiesInWindow(orderedTransactions, i, current.getAccountId(), 24);
                if (distinctCounterparties24h >= 3) {
                    score += 25.0;
                    reasons.add("rapid fan-out across counterparties within 24h");
                }

                if (hasReverseCycleInWindow(orderedTransactions, i, current, 48)) {
                    score += 20.0;
                    reasons.add("short transfer cycle pattern");
                }

                long senderCount = countUniqueSendersToCounterparty(orderedTransactions, i, current.getCounterpartyAccountId(), 7 * 24);
                if (senderCount >= 4) {
                    score += 20.0;
                    reasons.add("shared hub counterparty");
                }
            }

            if (isDebit(current.getTransactionType()) && current.getAmount() != null && current.getAmount() >= 50000.0) {
                score += 10.0;
                reasons.add("large debit amount");
            }

            int hour = current.getTransactionTime().getHour();
            if (isDebit(current.getTransactionType()) && hour >= 0 && hour < 5) {
                score += 10.0;
                reasons.add("off-hour debit activity");
            }

            score = Math.min(100.0, score);
            String reason = reasons.isEmpty() ? "graph patterns are stable" : String.join("; ", reasons);
            scoreByTransactionId.put(current.getId(), new GraphRiskMetrics(score, reason));
        }

        return scoreByTransactionId;
    }

    private long countPriorEdges(List<Transaction> transactions, int currentIndex, Transaction current) {
        long count = 0;
        for (int i = 0; i < currentIndex; i++) {
            Transaction previous = transactions.get(i);
            if (!isTransferLike(previous)) {
                continue;
            }
            if (Objects.equals(previous.getAccountId(), current.getAccountId())
                    && Objects.equals(previous.getCounterpartyAccountId(), current.getCounterpartyAccountId())) {
                count++;
            }
        }
        return count;
    }

    private long countDistinctCounterpartiesInWindow(List<Transaction> transactions, int currentIndex, Long accountId, long windowHours) {
        LocalDateTime currentTime = transactions.get(currentIndex).getTransactionTime();
        Map<Long, Boolean> distinctCounterparties = new HashMap<>();

        for (int i = 0; i < currentIndex; i++) {
            Transaction previous = transactions.get(i);
            if (!isTransferLike(previous) || previous.getCounterpartyAccountId() == null) {
                continue;
            }
            if (!Objects.equals(previous.getAccountId(), accountId)) {
                continue;
            }
            if (!isWithinWindow(previous.getTransactionTime(), currentTime, windowHours)) {
                continue;
            }
            distinctCounterparties.put(previous.getCounterpartyAccountId(), Boolean.TRUE);
        }

        return distinctCounterparties.size();
    }

    private boolean hasReverseCycleInWindow(List<Transaction> transactions, int currentIndex, Transaction current, long windowHours) {
        LocalDateTime currentTime = current.getTransactionTime();

        for (int i = 0; i < currentIndex; i++) {
            Transaction previous = transactions.get(i);
            if (!isTransferLike(previous)) {
                continue;
            }
            if (!isWithinWindow(previous.getTransactionTime(), currentTime, windowHours)) {
                continue;
            }
            boolean reverseMatch = Objects.equals(previous.getAccountId(), current.getCounterpartyAccountId())
                    && Objects.equals(previous.getCounterpartyAccountId(), current.getAccountId());
            if (reverseMatch) {
                return true;
            }
        }

        return false;
    }

    private long countUniqueSendersToCounterparty(List<Transaction> transactions, int currentIndex, Long counterpartyAccountId, long windowHours) {
        LocalDateTime currentTime = transactions.get(currentIndex).getTransactionTime();
        Map<Long, Boolean> uniqueSenders = new HashMap<>();

        for (int i = 0; i < currentIndex; i++) {
            Transaction previous = transactions.get(i);
            if (!isTransferLike(previous) || previous.getCounterpartyAccountId() == null) {
                continue;
            }
            if (!Objects.equals(previous.getCounterpartyAccountId(), counterpartyAccountId)) {
                continue;
            }
            if (!isWithinWindow(previous.getTransactionTime(), currentTime, windowHours)) {
                continue;
            }
            uniqueSenders.put(previous.getAccountId(), Boolean.TRUE);
        }

        return uniqueSenders.size();
    }

    private boolean isWithinWindow(LocalDateTime candidate, LocalDateTime reference, long windowHours) {
        if (candidate == null || reference == null || candidate.isAfter(reference)) {
            return false;
        }
        long hours = Duration.between(candidate, reference).toHours();
        return hours <= windowHours;
    }

    private boolean isTransferLike(Transaction transaction) {
        if (transaction == null || transaction.getTransactionType() == null) {
            return false;
        }
        return transaction.getTransactionType() == Transaction.TransactionType.TRANSFER
                || transaction.getTransactionType() == Transaction.TransactionType.PAYMENT;
    }

    private boolean isDebit(Transaction.TransactionType transactionType) {
        if (transactionType == null) {
            return false;
        }
        return transactionType == Transaction.TransactionType.WITHDRAWAL
                || transactionType == Transaction.TransactionType.TRANSFER
                || transactionType == Transaction.TransactionType.PAYMENT;
    }

    public record GraphRiskMetrics(
            double score,
            String reason
    ) {
    }
}
