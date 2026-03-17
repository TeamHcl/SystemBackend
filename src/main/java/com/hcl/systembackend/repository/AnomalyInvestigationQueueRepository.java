package com.hcl.systembackend.repository;

import com.hcl.systembackend.entity.AnomalyInvestigationQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyInvestigationQueueRepository extends JpaRepository<AnomalyInvestigationQueue, Long> {
    boolean existsByTransactionId(Long transactionId);

    List<AnomalyInvestigationQueue> findAllByOrderByCreatedAtDesc();

    long countByStatusIgnoreCase(String status);
}
