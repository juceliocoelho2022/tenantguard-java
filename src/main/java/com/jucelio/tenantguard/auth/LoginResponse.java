package com.jucelio.tenantguard.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String tenantId
) {}
