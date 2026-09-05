package com.jucelio.tenantguard.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationMetrics {

    private final MeterRegistry meterRegistry;

    public AuthenticationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(String operation, String outcome) {
        Counter.builder("tenantguard.auth.operations")
                .description("Authentication operations grouped by operation and outcome")
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public void recordRateLimitBlocked(String endpoint) {
        Counter.builder("tenantguard.auth.rate_limit.blocked")
                .description("Authentication requests blocked by rate limiting")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }
}
