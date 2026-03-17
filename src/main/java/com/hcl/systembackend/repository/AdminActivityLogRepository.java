package com.hcl.systembackend.repository;

import com.hcl.systembackend.entity.AdminActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {
    Page<AdminActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
