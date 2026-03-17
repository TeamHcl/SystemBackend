package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.AuthResponse;
import com.hcl.systembackend.dto.DelegatedAdminProfile;
import com.hcl.systembackend.dto.SigninRequest;
import com.hcl.systembackend.dto.SignupRequest;
import com.hcl.systembackend.entity.Admin;
import com.hcl.systembackend.repository.AdminRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminRepository adminRepository;
    private final PasswordService passwordService;
    private final MockBankAdminAuthClient mockBankAdminAuthClient;
    private final AdminActivityLogService adminActivityLogService;
    private final Map<String, AdminSession> activeTokens = new ConcurrentHashMap<>();

    public AdminAuthService(
            AdminRepository adminRepository,
            PasswordService passwordService,
            MockBankAdminAuthClient mockBankAdminAuthClient,
            AdminActivityLogService adminActivityLogService
    ) {
        this.adminRepository = adminRepository;
        this.passwordService = passwordService;
        this.mockBankAdminAuthClient = mockBankAdminAuthClient;
        this.adminActivityLogService = adminActivityLogService;
    }

    public String registerAdmin(SignupRequest request) {
        validateSignup(request);
        String result = mockBankAdminAuthClient.registerAdmin(request);
        syncShadowAdmin(new DelegatedAdminProfile(request.email(), request.name()));
        return result;
    }

    public AuthResponse login(SigninRequest request) {
        validateSignin(request);
        DelegatedAdminProfile delegatedAdmin = mockBankAdminAuthClient.authenticateAdmin(request);
        Admin admin = syncShadowAdmin(delegatedAdmin);

        String token = UUID.randomUUID().toString();
        activeTokens.put(token, new AdminSession(admin.getAdminId(), delegatedAdmin.delegatedToken()));
        adminActivityLogService.record(admin.getAdminId(), "ADMIN_LOGIN", "Admin authenticated using delegated MockBank credentials");
        return new AuthResponse(token, "Admin authenticated successfully");
    }

    public boolean isAuthorizedAdmin(HttpServletRequest request) {
        String token = extractBearerToken(request);
        return token != null && activeTokens.containsKey(token);
    }

    public Integer requireAdminId(HttpServletRequest request) {
        AdminSession session = requireAdminSession(request);
        if (session == null || session.adminId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid admin bearer token is required");
        }
        return session.adminId();
    }

    public String getDelegatedMockBankToken(HttpServletRequest request) {
        AdminSession session = requireAdminSession(request);
        return session.delegatedMockBankToken();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length()).trim();
    }

    private AdminSession requireAdminSession(HttpServletRequest request) {
        String token = extractBearerToken(request);
        AdminSession session = token == null ? null : activeTokens.get(token);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid admin bearer token is required");
        }
        return session;
    }

    private Admin syncShadowAdmin(DelegatedAdminProfile delegatedAdmin) {
        String normalizedEmail = delegatedAdmin.email().trim().toLowerCase();
        String displayName = isBlank(delegatedAdmin.name()) ? normalizedEmail : delegatedAdmin.name().trim();

        return adminRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existingAdmin -> updateShadowAdmin(existingAdmin, normalizedEmail, displayName))
                .orElseGet(() -> createShadowAdmin(normalizedEmail, displayName));
    }

    private Admin updateShadowAdmin(Admin admin, String normalizedEmail, String displayName) {
        boolean requiresSave = false;

        if (!Objects.equals(admin.getName(), displayName)) {
            admin.setName(displayName);
            requiresSave = true;
        }
        if (!normalizedEmail.equalsIgnoreCase(admin.getEmail())) {
            admin.setEmail(normalizedEmail);
            requiresSave = true;
        }
        if (isBlank(admin.getPassword())) {
            admin.setPassword(passwordService.hash(UUID.randomUUID().toString()));
            requiresSave = true;
        }
        if (admin.getCreatedAt() == null) {
            admin.setCreatedAt(Timestamp.from(Instant.now()));
            requiresSave = true;
        }

        return requiresSave ? adminRepository.save(admin) : admin;
    }

    private Admin createShadowAdmin(String normalizedEmail, String displayName) {
        Admin admin = new Admin();
        admin.setName(displayName);
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordService.hash(UUID.randomUUID().toString()));
        admin.setCreatedAt(Timestamp.from(Instant.now()));
        return adminRepository.save(admin);
    }

    private void validateSignup(SignupRequest request) {
        if (request == null || isBlank(request.name()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email, and password are required");
        }
    }

    private void validateSignin(SigninRequest request) {
        if (request == null || isBlank(request.email()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record AdminSession(
            Integer adminId,
            String delegatedMockBankToken
    ) {
    }
}
