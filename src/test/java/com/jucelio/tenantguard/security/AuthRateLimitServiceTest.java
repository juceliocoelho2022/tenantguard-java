package com.jucelio.tenantguard.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AuthRateLimitServiceTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-04T12:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void shouldAllowRequestsUntilLoginLimitIsReached() {
        AuthRateLimitService service = service(2, 4, 60);

        var first = service.checkLogin("127.0.0.1");
        var second = service.checkLogin("127.0.0.1");

        assertTrue(first.allowed());
        assertEquals(1, first.remaining());
        assertTrue(second.allowed());
        assertEquals(0, second.remaining());
    }

    @Test
    void shouldRejectLoginWhenLimitIsExceeded() {
        AuthRateLimitService service = service(2, 4, 60);

        service.checkLogin("127.0.0.1");
        service.checkLogin("127.0.0.1");
        var blocked = service.checkLogin("127.0.0.1");

        assertFalse(blocked.allowed());
        assertEquals(2, blocked.limit());
        assertEquals(0, blocked.remaining());
        assertEquals(60, blocked.retryAfterSeconds());
    }

    @Test
    void loginAndRefreshMustUseIndependentBuckets() {
        AuthRateLimitService service = service(1, 1, 60);

        assertTrue(service.checkLogin("127.0.0.1").allowed());
        assertFalse(service.checkLogin("127.0.0.1").allowed());

        assertTrue(service.checkRefresh("127.0.0.1").allowed());
        assertFalse(service.checkRefresh("127.0.0.1").allowed());
    }

    private AuthRateLimitService service(int loginLimit, int refreshLimit, long windowSeconds) {
        return new AuthRateLimitService(
                loginLimit,
                refreshLimit,
                windowSeconds,
                new InMemoryRateLimitStore(clock)
        );
    }
}
