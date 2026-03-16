package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.DelegatedAdminProfile;
import com.hcl.systembackend.dto.SigninRequest;
import com.hcl.systembackend.dto.SignupRequest;

public interface MockBankAdminAuthClient {
    String registerAdmin(SignupRequest request);

    DelegatedAdminProfile authenticateAdmin(SigninRequest request);
}