package com.jucelio.tenantguard.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String tenantId
) {}
