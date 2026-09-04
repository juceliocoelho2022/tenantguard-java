package com.jucelio.tenantguard.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimitService {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int loginMaxAttempts;
    private final int refreshMaxAttempts;
    private final long windowSeconds;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitService(
            @Value("${app.security.rate-limit.login.max-attempts:5}") int loginMaxAttempts,
            @Value("${app.security.rate-limit.refresh.max-attempts:10}") int refreshMaxAttempts,
            @Value("${app.security.rate-limit.window-seconds:60}") long windowSeconds) {
        this(loginMaxAttempts, refreshMaxAttempts, windowSeconds, Clock.systemUTC());
    }

    AuthRateLimitService(
            int loginMaxAttempts,
            int refreshMaxAttempts,
            long windowSeconds,
            Clock clock) {
        if (loginMaxAttempts < 1 || refreshMaxAttempts < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Configuração de rate limit inválida.");
        }
        this.loginMaxAttempts = loginMaxAttempts;
        this.refreshMaxAttempts = refreshMaxAttempts;
        this.windowSeconds = windowSeconds;
        this.clock = clock;
    }

    public RateLimitDecision checkLogin(String clientId) {
        return check("login", clientId, loginMaxAttempts);
    }

    public RateLimitDecision checkRefresh(String clientId) {
        return check("refresh", clientId, refreshMaxAttempts);
    }

    private RateLimitDecision check(String scope, String clientId, int limit) {
        long now = Instant.now(clock).getEpochSecond();
        String key = scope + ':' + clientId;

        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.resetAtEpochSecond()) {
                return new Window(1, now + windowSeconds);
            }
            return new Window(current.count() + 1, current.resetAtEpochSecond());
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtEpochSecond());
        }

        boolean allowed = window.count() <= limit;
        int remaining = Math.max(0, limit - window.count());
        long retryAfter = Math.max(1, window.resetAtEpochSecond() - now);

        return new RateLimitDecision(allowed, limit, remaining, retryAfter);
    }

    private record Window(int count, long resetAtEpochSecond) {}

    public record RateLimitDecision(
            boolean allowed,
            int limit,
            int remaining,
            long retryAfterSeconds) {}
}
