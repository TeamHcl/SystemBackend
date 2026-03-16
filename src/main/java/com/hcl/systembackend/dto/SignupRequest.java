package com.hcl.systembackend.dto;

public record SignupRequest(
        String name,
        String email,
        String password
) {
}
