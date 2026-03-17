package com.hcl.systembackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @Column(name = "source_transaction_id", unique = true)
    private Long sourceTransactionId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "counterparty_account_id")
    private Long counterpartyAccountId;

    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionType transactionType;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    @Column(name = "channel")
    private String channel;

    public Transaction() {}

    public Transaction(Long accountId, Double amount, TransactionType transactionType) {
        this.accountId = accountId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getSourceTransactionId() { return sourceTransactionId; }
    public Long getAccountId() { return accountId; }
    public Long getCounterpartyAccountId() { return counterpartyAccountId; }
    public Double getAmount() { return amount; }
    public TransactionType getTransactionType() { return transactionType; }
    public LocalDateTime getTransactionTime() { return transactionTime; }
    public String getChannel() { return channel; }

    public void setSourceTransactionId(Long sourceTransactionId) { this.sourceTransactionId = sourceTransactionId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public void setCounterpartyAccountId(Long counterpartyAccountId) { this.counterpartyAccountId = counterpartyAccountId; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }
    public void setChannel(String channel) { this.channel = channel; }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, OTHER
    }
}
