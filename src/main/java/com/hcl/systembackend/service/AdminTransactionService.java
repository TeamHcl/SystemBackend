package com.hcl.systembackend.service;

import com.hcl.systembackend.entity.Transaction;
import com.hcl.systembackend.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminTransactionService {
    private final TransactionRepository transactionRepository;

    @Autowired
    public AdminTransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<Transaction> getRawTransactions() {
        // For now, return all as raw; can be customized for raw logs
        return transactionRepository.findAll();
    }
}

