package com.hcl.systembackend.dto;

public record SigninRequest(
        String email,
        String password
) {
}
