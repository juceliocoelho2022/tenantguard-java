package com.jucelio.tenantguard.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {

    private final int loginMaxAttempts;
    private final int refreshMaxAttempts;
    private final long windowSeconds;
    private final RateLimitStore rateLimitStore;

    public AuthRateLimitService(
            @Value("${app.security.rate-limit.login.max-attempts:5}") int loginMaxAttempts,
            @Value("${app.security.rate-limit.refresh.max-attempts:10}") int refreshMaxAttempts,
            @Value("${app.security.rate-limit.window-seconds:60}") long windowSeconds,
            RateLimitStore rateLimitStore) {
        if (loginMaxAttempts < 1 || refreshMaxAttempts < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Configuração de rate limit inválida.");
        }
        this.loginMaxAttempts = loginMaxAttempts;
        this.refreshMaxAttempts = refreshMaxAttempts;
        this.windowSeconds = windowSeconds;
        this.rateLimitStore = rateLimitStore;
    }

    public RateLimitDecision checkLogin(String clientId) {
        return check("login", clientId, loginMaxAttempts);
    }

    public RateLimitDecision checkRefresh(String clientId) {
        return check("refresh", clientId, refreshMaxAttempts);
    }

    private RateLimitDecision check(String scope, String clientId, int limit) {
        RateLimitStore.Bucket bucket = rateLimitStore.consume(scope + ':' + clientId, windowSeconds);
        boolean allowed = bucket.count() <= limit;
        int remaining = Math.max(0, limit - Math.toIntExact(Math.min(bucket.count(), Integer.MAX_VALUE)));

        return new RateLimitDecision(
                allowed,
                limit,
                remaining,
                Math.max(1, bucket.retryAfterSeconds())
        );
    }

    public record RateLimitDecision(
            boolean allowed,
            int limit,
            int remaining,
            long retryAfterSeconds) {}
}
