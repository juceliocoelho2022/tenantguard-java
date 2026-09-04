package com.jucelio.tenantguard.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

@Component
public class JwtTenantResolver {

    private final JwtService jwtService;

    public JwtTenantResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public AuthenticatedUser resolve(String token) {
        Claims claims = jwtService.parseAccessToken(token);

        String username = claims.getSubject();
        String tenantId = claims.get("tenant_id", String.class);
        String role = claims.get("role", String.class);

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("JWT sem subject.");
        }

        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("JWT sem claim tenant_id.");
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("JWT sem claim role.");
        }

        return new AuthenticatedUser(username, tenantId, role);
    }
}
