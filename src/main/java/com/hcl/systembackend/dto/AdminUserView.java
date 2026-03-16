package com.hcl.systembackend.dto;

public record AdminUserView(
        Long id,
        String name,
        String email,
        String role
) {
}
