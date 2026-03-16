package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.AuthResponse;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminRepository adminRepository;
    private final PasswordService passwordService;
    private final Map<String, Integer> activeTokens = new ConcurrentHashMap<>();

    public AdminAuthService(AdminRepository adminRepository, PasswordService passwordService) {
        this.adminRepository = adminRepository;
        this.passwordService = passwordService;
    }

    public String registerAdmin(SignupRequest request) {
        validateSignup(request);
        adminRepository.findByEmailIgnoreCase(request.email()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin email is already registered");
        });

        Admin admin = new Admin();
        admin.setName(request.name().trim());
        admin.setEmail(request.email().trim().toLowerCase());
        admin.setPassword(passwordService.hash(request.password()));
        admin.setCreatedAt(Timestamp.from(Instant.now()));
        adminRepository.save(admin);
        return "Admin registered successfully";
    }

    public AuthResponse login(SigninRequest request) {
        validateSignin(request);
        Admin admin = adminRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials"));

        if (!passwordService.matches(request.password(), admin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }

        String token = UUID.randomUUID().toString();
        activeTokens.put(token, admin.getAdminId());
        return new AuthResponse(token, "Admin authenticated successfully");
    }

    public boolean isAuthorizedAdmin(HttpServletRequest request) {
        String token = extractBearerToken(request);
        return token != null && activeTokens.containsKey(token);
    }

    public Integer requireAdminId(HttpServletRequest request) {
        String token = extractBearerToken(request);
        Integer adminId = token == null ? null : activeTokens.get(token);
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid admin bearer token is required");
        }
        return adminId;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length()).trim();
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
}
