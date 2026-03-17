package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.AdminActivityLogItem;
import com.hcl.systembackend.entity.AdminActivityLog;
import com.hcl.systembackend.repository.AdminActivityLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminActivityLogService {
    private final AdminActivityLogRepository adminActivityLogRepository;

    public AdminActivityLogService(AdminActivityLogRepository adminActivityLogRepository) {
        this.adminActivityLogRepository = adminActivityLogRepository;
    }

    public void record(Integer adminId, String action, String details) {
        AdminActivityLog log = new AdminActivityLog();
        log.setAdminId(adminId);
        log.setAction(action == null || action.isBlank() ? "UNKNOWN_ACTION" : action.trim());
        log.setDetails(details == null ? "" : details.trim());
        log.setCreatedAt(LocalDateTime.now());
        adminActivityLogRepository.save(log);
    }

    public List<AdminActivityLogItem> getRecent(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        return adminActivityLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .map(log -> new AdminActivityLogItem(
                        log.getId(),
                        log.getAdminId(),
                        log.getAction(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
