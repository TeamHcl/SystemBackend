package com.hcl.systembackend.repository;

import com.hcl.systembackend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

}

