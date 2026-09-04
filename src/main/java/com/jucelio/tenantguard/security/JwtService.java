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
import java.util.UUID;

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
        return generateToken(user, ACCESS_TOKEN_TYPE, accessExpirationSeconds, null);
    }

    public String generateRefreshToken(AuthenticatedUser user) {
        return generateRefreshToken(user, UUID.randomUUID().toString());
    }

    public String generateRefreshToken(AuthenticatedUser user, String jti) {
        return generateToken(user, REFRESH_TOKEN_TYPE, refreshExpirationSeconds, jti);
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
        return parseRefreshTokenDetails(token).user();
    }

    public RefreshTokenDetails parseRefreshTokenDetails(String token) {
        Claims claims = parse(token);
        requireTokenType(claims, REFRESH_TOKEN_TYPE);

        String username = claims.getSubject();
        String tenantId = claims.get("tenant_id", String.class);
        String role = claims.get("role", String.class);
        String jti = claims.getId();
        Date expiration = claims.getExpiration();

        if (username == null || username.isBlank()
                || tenantId == null || tenantId.isBlank()
                || role == null || role.isBlank()
                || jti == null || jti.isBlank()
                || expiration == null) {
            throw new IllegalArgumentException("Refresh token sem claims obrigatórias.");
        }

        return new RefreshTokenDetails(
                jti,
                new AuthenticatedUser(username, tenantId, role),
                expiration.toInstant()
        );
    }

    public long accessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    private String generateToken(
            AuthenticatedUser user,
            String tokenType,
            long expirationSeconds,
            String jti) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
                .subject(user.username())
                .claim("tenant_id", user.tenantId())
                .claim("role", user.role())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)));

        if (jti != null) {
            builder.id(jti);
        }

        return builder
                .signWith(key)
                .compact();
    }

    private void requireTokenType(Claims claims, String expectedType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.equals(tokenType)) {
            throw new IllegalArgumentException("Tipo de token inválido.");
        }
    }

    public record RefreshTokenDetails(
            String jti,
            AuthenticatedUser user,
            Instant expiresAt) {
    }
}
