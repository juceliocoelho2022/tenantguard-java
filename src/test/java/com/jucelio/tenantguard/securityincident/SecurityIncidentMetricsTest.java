package com.jucelio.tenantguard.securityincident;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityIncidentMetricsTest {

    @Test
    void shouldRecordLifecycleCountersAndClosureDurationWithoutHighCardinalityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SecurityIncidentMetrics metrics = new SecurityIncidentMetrics(registry);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-07T12:00:00Z");
        SecurityIncident incident = SecurityIncident.open(
                "TENANT_A",
                SecurityIncidentSeverity.HIGH,
                70,
                "abc123",
                createdAt
        );

        metrics.opened(incident);
        metrics.deduplicated(incident);
        incident.startInvestigation(createdAt.plusMinutes(5));
        metrics.investigationStarted(incident);
        incident.resolve("Mitigated", createdAt.plusMinutes(30));
        metrics.resolved(incident);

        assertEquals(1.0, registry.get("tenantguard.security.incidents.opened")
                .tag("severity", "high").counter().count());
        assertEquals(1.0, registry.get("tenantguard.security.incidents.deduplicated")
                .tag("severity", "high").counter().count());
        assertEquals(1.0, registry.get("tenantguard.security.incidents.investigation.started")
                .tag("severity", "high").counter().count());
        assertEquals(1.0, registry.get("tenantguard.security.incidents.resolved")
                .tag("severity", "high").counter().count());
        assertEquals(1L, registry.get("tenantguard.security.incidents.closure.duration")
                .tag("outcome", "resolved")
                .tag("severity", "high")
                .timer().count());
        assertEquals(1800.0, registry.get("tenantguard.security.incidents.closure.duration")
                .tag("outcome", "resolved")
                .tag("severity", "high")
                .timer().totalTime(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void shouldRecordDismissedClosure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SecurityIncidentMetrics metrics = new SecurityIncidentMetrics(registry);
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-07T12:00:00Z");
        SecurityIncident incident = SecurityIncident.open(
                "TENANT_B",
                SecurityIncidentSeverity.MEDIUM,
                45,
                "xyz789",
                createdAt
        );

        incident.dismiss("False positive", createdAt.plusMinutes(10));
        metrics.dismissed(incident);

        assertEquals(1.0, registry.get("tenantguard.security.incidents.dismissed")
                .tag("severity", "medium").counter().count());
        assertEquals(1L, registry.get("tenantguard.security.incidents.closure.duration")
                .tag("outcome", "dismissed")
                .tag("severity", "medium")
                .timer().count());
    }
}
