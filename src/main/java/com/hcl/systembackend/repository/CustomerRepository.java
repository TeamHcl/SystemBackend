package com.hcl.systembackend.repository;

import com.hcl.systembackend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    Optional<Customer> findBySourceUserId(Long sourceUserId);
}

