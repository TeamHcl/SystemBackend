package com.hcl.systembackend.repository;

import com.hcl.systembackend.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByEmailIgnoreCase(String email);
}
