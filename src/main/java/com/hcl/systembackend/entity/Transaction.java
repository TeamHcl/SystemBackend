package com.hcl.systembackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;
    private Double amount;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    private LocalDateTime transactionTime;

    public Transaction() {}

    public Transaction(Long accountId, Double amount, TransactionType transactionType) {
        this.accountId = accountId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public Double getAmount() { return amount; }
    public TransactionType getTransactionType() { return transactionType; }
    public LocalDateTime getTransactionTime() { return transactionTime; }

    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, OTHER
    }
}
