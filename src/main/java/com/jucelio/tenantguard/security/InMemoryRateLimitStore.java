package com.jucelio.tenantguard.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(
        name = "app.security.rate-limit.backend",
        havingValue = "memory",
        matchIfMissing = true)
public class InMemoryRateLimitStore implements RateLimitStore {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitStore() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimitStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Bucket consume(String key, long windowSeconds) {
        long now = Instant.now(clock).getEpochSecond();

        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.resetAtEpochSecond()) {
                return new Window(1, now + windowSeconds);
            }
            return new Window(current.count() + 1, current.resetAtEpochSecond());
        });

        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtEpochSecond());
        }

        long retryAfter = Math.max(1, window.resetAtEpochSecond() - now);
        return new Bucket(window.count(), retryAfter);
    }

    private record Window(long count, long resetAtEpochSecond) {}
}
