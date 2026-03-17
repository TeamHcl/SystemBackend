package com.hcl.systembackend.repository;

import com.hcl.systembackend.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findBySourceAccountId(Long sourceAccountId);
}

