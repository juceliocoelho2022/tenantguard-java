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
        Claims claims = jwtService.parse(token);

        String username = claims.getSubject();
        String tenantId = claims.get("tenant_id", String.class);
        String role = claims.get("role", String.class);

        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("JWT sem claim tenant_id.");
        }

        return new AuthenticatedUser(
                username,
                tenantId,
                role == null ? "USER" : role
        );
    }
}
