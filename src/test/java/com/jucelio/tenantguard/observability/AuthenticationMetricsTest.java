package com.jucelio.tenantguard.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthenticationMetricsTest {

    @Test
    void shouldRecordAuthenticationOperationWithBoundedTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthenticationMetrics metrics = new AuthenticationMetrics(registry);

        metrics.record("login", "success");
        metrics.record("login", "success");
        metrics.record("login", "failure");

        assertEquals(2.0, registry.get("tenantguard.auth.operations")
                .tag("operation", "login")
                .tag("outcome", "success")
                .counter()
                .count());
        assertEquals(1.0, registry.get("tenantguard.auth.operations")
                .tag("operation", "login")
                .tag("outcome", "failure")
                .counter()
                .count());
    }

    @Test
    void shouldRecordRateLimitBlocksByEndpoint() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AuthenticationMetrics metrics = new AuthenticationMetrics(registry);

        metrics.recordRateLimitBlocked("refresh");

        assertEquals(1.0, registry.get("tenantguard.auth.rate_limit.blocked")
                .tag("endpoint", "refresh")
                .counter()
                .count());
    }
}
