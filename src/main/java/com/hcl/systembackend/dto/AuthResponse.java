package com.hcl.systembackend.dto;

public record AuthResponse(
        String token,
        String message
) {
}
