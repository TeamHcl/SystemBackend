package com.hcl.systembackend.dto;

public record DelegatedAdminProfile(
        String email,
                String name,
                String delegatedToken
) {
        public DelegatedAdminProfile(String email, String name) {
                this(email, name, null);
        }
}