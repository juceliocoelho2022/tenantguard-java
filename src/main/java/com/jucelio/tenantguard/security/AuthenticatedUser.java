package com.jucelio.tenantguard.security;

public record AuthenticatedUser(
        String username,
        String tenantId,
        String role
) {}
