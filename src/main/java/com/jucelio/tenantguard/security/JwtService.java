package com.jucelio.tenantguard.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_CLAIM = "token_type";
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:3600}") long accessExpirationSeconds,
            @Value("${app.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String generateToken(AuthenticatedUser user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(AuthenticatedUser user) {
        return generateToken(user, ACCESS_TOKEN_TYPE, accessExpirationSeconds);
    }

    public String generateRefreshToken(AuthenticatedUser user) {
        return generateToken(user, REFRESH_TOKEN_TYPE, refreshExpirationSeconds);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        requireTokenType(claims, ACCESS_TOKEN_TYPE);
        return claims;
    }

    public AuthenticatedUser parseRefreshToken(String token) {
        Claims claims = parse(token);
        requireTokenType(claims, REFRESH_TOKEN_TYPE);

        String username = claims.getSubject();
        String tenantId = claims.get("tenant_id", String.class);
        String role = claims.get("role", String.class);

        if (username == null || username.isBlank()
                || tenantId == null || tenantId.isBlank()
                || role == null || role.isBlank()) {
            throw new IllegalArgumentException("Refresh token sem claims obrigatórias.");
        }

        return new AuthenticatedUser(username, tenantId, role);
    }

    public long accessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    private String generateToken(AuthenticatedUser user, String tokenType, long expirationSeconds) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.username())
                .claim("tenant_id", user.tenantId())
                .claim("role", user.role())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    private void requireTokenType(Claims claims, String expectedType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.equals(tokenType)) {
            throw new IllegalArgumentException("Tipo de token inválido.");
        }
    }
}
