package com.jucelio.tenantguard.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitService.class);

    private final int loginMaxAttempts;
    private final int refreshMaxAttempts;
    private final long windowSeconds;
    private final RateLimitStore rateLimitStore;
    private final boolean failOpen;

    @Autowired
    public AuthRateLimitService(
            @Value("${app.security.rate-limit.login.max-attempts:5}") int loginMaxAttempts,
            @Value("${app.security.rate-limit.refresh.max-attempts:10}") int refreshMaxAttempts,
            @Value("${app.security.rate-limit.window-seconds:60}") long windowSeconds,
            RateLimitStore rateLimitStore,
            @Value("${app.security.rate-limit.fail-open:true}") boolean failOpen) {
        if (loginMaxAttempts < 1 || refreshMaxAttempts < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Configuração de rate limit inválida.");
        }
        this.loginMaxAttempts = loginMaxAttempts;
        this.refreshMaxAttempts = refreshMaxAttempts;
        this.windowSeconds = windowSeconds;
        this.rateLimitStore = rateLimitStore;
        this.failOpen = failOpen;
    }

    AuthRateLimitService(int loginMaxAttempts, int refreshMaxAttempts, long windowSeconds, RateLimitStore rateLimitStore) {
        this(loginMaxAttempts, refreshMaxAttempts, windowSeconds, rateLimitStore, true);
    }

    public RateLimitDecision checkLogin(String clientId) {
        return check("login", clientId, loginMaxAttempts);
    }

    public RateLimitDecision checkRefresh(String clientId) {
        return check("refresh", clientId, refreshMaxAttempts);
    }

    private RateLimitDecision check(String scope, String clientId, int limit) {
        final RateLimitStore.Bucket bucket;
        try {
            bucket = rateLimitStore.consume(scope + ':' + clientId, windowSeconds);
        } catch (RuntimeException ex) {
            if (!failOpen) {
                throw ex;
            }

            log.warn("Rate-limit backend unavailable; allowing request because fail-open is enabled. scope={}", scope, ex);
            return new RateLimitDecision(true, limit, limit, 1);
        }

        boolean allowed = bucket.count() <= limit;
        int remaining = (int) Math.max(0L, (long) limit - bucket.count());

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
