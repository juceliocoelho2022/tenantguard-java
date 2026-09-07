package com.jucelio.tenantguard.securityincident;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
public class SecurityIncidentMetrics {

    private final MeterRegistry meterRegistry;

    public SecurityIncidentMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void opened(SecurityIncident incident) {
        increment("tenantguard.security.incidents.opened", "Security incidents opened", incident);
    }

    public void deduplicated(SecurityIncident incident) {
        increment("tenantguard.security.incidents.deduplicated", "Security incident creation attempts deduplicated", incident);
    }

    public void investigationStarted(SecurityIncident incident) {
        increment("tenantguard.security.incidents.investigation.started", "Security incidents moved to investigation", incident);
    }

    public void resolved(SecurityIncident incident) {
        increment("tenantguard.security.incidents.resolved", "Security incidents resolved", incident);
        recordClosureDuration(incident, "resolved");
    }

    public void dismissed(SecurityIncident incident) {
        increment("tenantguard.security.incidents.dismissed", "Security incidents dismissed", incident);
        recordClosureDuration(incident, "dismissed");
    }

    private void increment(String name, String description, SecurityIncident incident) {
        Counter.builder(name)
                .description(description)
                .tag("severity", incident.getSeverity().name().toLowerCase())
                .register(meterRegistry)
                .increment();
    }

    private void recordClosureDuration(SecurityIncident incident, String outcome) {
        OffsetDateTime resolvedAt = incident.getResolvedAt();
        if (resolvedAt == null || resolvedAt.isBefore(incident.getCreatedAt())) {
            return;
        }

        Duration duration = Duration.between(incident.getCreatedAt(), resolvedAt);

        Timer.builder("tenantguard.security.incidents.closure.duration")
                .description("Time from security incident creation until closure")
                .tag("outcome", outcome)
                .tag("severity", incident.getSeverity().name().toLowerCase())
                .register(meterRegistry)
                .record(duration);
    }
}
