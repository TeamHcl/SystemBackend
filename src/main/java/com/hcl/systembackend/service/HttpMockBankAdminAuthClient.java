package com.hcl.systembackend.service;

import com.hcl.systembackend.dto.DelegatedAdminProfile;
import com.hcl.systembackend.dto.SigninRequest;
import com.hcl.systembackend.dto.SignupRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HttpMockBankAdminAuthClient implements MockBankAdminAuthClient {
    private final RestClient restClient;

    public HttpMockBankAdminAuthClient(@Value("${mockbank.base-url:http://localhost:8080}") String mockBankBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(normalizeBaseUrl(mockBankBaseUrl))
                .build();
    }

    @Override
    public String registerAdmin(SignupRequest request) {
        try {
            return restClient.post()
                    .uri("/api/admin/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw mapRemoteException(exception, "MockBank admin registration failed");
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MockBank admin service is unavailable", exception);
        }
    }

    @Override
    public DelegatedAdminProfile authenticateAdmin(SigninRequest request) {
        try {
            RemoteAdminAuthResponse response = restClient.post()
                    .uri("/api/auth/admin/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RemoteAdminAuthResponse.class);

            if (response == null || isBlank(response.email())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MockBank admin authentication failed");
            }

            return new DelegatedAdminProfile(
                    response.email().trim().toLowerCase(),
                    response.name(),
                    response.token()
            );
        } catch (RestClientResponseException exception) {
            throw mapRemoteException(exception, "MockBank admin authentication failed");
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "MockBank admin service is unavailable", exception);
        }
    }

    private ResponseStatusException mapRemoteException(RestClientResponseException exception, String fallbackMessage) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return new ResponseStatusException(status, extractMessage(exception.getResponseBodyAsString(), fallbackMessage), exception);
    }

    private String extractMessage(String responseBody, String fallbackMessage) {
        if (isBlank(responseBody)) {
            return fallbackMessage;
        }

        String marker = "\"message\":";
        int markerIndex = responseBody.indexOf(marker);
        if (markerIndex >= 0) {
            int start = responseBody.indexOf('"', markerIndex + marker.length());
            if (start >= 0) {
                int end = responseBody.indexOf('"', start + 1);
                if (end > start) {
                    return responseBody.substring(start + 1, end);
                }
            }
        }

        return responseBody;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (isBlank(baseUrl)) {
            return "http://localhost:8080";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record RemoteAdminAuthResponse(
            String token,
            String message,
            String email,
            String name
    ) {
    }
}