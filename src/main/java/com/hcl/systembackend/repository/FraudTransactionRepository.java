package com.hcl.systembackend.repository;

import com.hcl.systembackend.entity.FraudTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FraudTransactionRepository extends JpaRepository<FraudTransaction, Long> {
    boolean existsByTransactionId(Long transactionId);
    Optional<FraudTransaction> findByTransactionId(Long transactionId);
}
