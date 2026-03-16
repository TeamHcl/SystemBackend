package com.hcl.systembackend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_transactions")
public class FraudTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fraud_id")
    private Long fraudId;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private Long transactionId;

    @Column(name = "fraud_type")
    private String fraudType;

    @Column(name = "fraud_reason")
    private String fraudReason;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    public Long getFraudId() {
        return fraudId;
    }

    public void setFraudId(Long fraudId) {
        this.fraudId = fraudId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getFraudType() {
        return fraudType;
    }

    public void setFraudType(String fraudType) {
        this.fraudType = fraudType;
    }

    public String getFraudReason() {
        return fraudReason;
    }

    public void setFraudReason(String fraudReason) {
        this.fraudReason = fraudReason;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
}
