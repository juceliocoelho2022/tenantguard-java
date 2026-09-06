package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditService;
import com.jucelio.tenantguard.security.audit.SecurityEventService;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class SecurityIntelligenceService {

    public static final int DEFAULT_LOOKBACK_HOURS = 24;
    public static final int MIN_LOOKBACK_HOURS = 1;
    public static final int MAX_LOOKBACK_HOURS = 168;
    public static final int EVENT_CAP = 500;

    private final AuditService auditService;
    private final SecurityEventService securityEventService;
    private final SecurityAnalysisProvider analysisProvider;

    public SecurityIntelligenceService(
            AuditService auditService,
            SecurityEventService securityEventService,
            SecurityAnalysisProvider analysisProvider
    ) {
        this.auditService = auditService;
        this.securityEventService = securityEventService;
        this.analysisProvider = analysisProvider;
    }

    public SecurityAnalysis analyzeCurrentTenant() {
        return analyzeCurrentTenant(DEFAULT_LOOKBACK_HOURS);
    }

    public SecurityAnalysis analyzeCurrentTenant(int lookbackHours) {
        validateLookbackHours(lookbackHours);

        String tenantId = TenantContext.getTenant();

        List<SecurityEvidence> events = Stream.concat(
                        auditService.findCurrentTenantEvents(lookbackHours, EVENT_CAP)
                                .stream()
                                .map(SecurityEvidence::fromAudit),
                        securityEventService.findCurrentTenantEvents(lookbackHours, EVENT_CAP)
                                .stream()
                                .map(SecurityEvidence::fromSecurity)
                )
                .sorted(Comparator.comparing(SecurityEvidence::createdAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(EVENT_CAP)
                .toList();

        boolean containsAnotherTenant = events.stream()
                .anyMatch(event -> !tenantId.equals(event.tenantId()));

        if (containsAnotherTenant) {
            throw new IllegalStateException("Security intelligence received events from another tenant.");
        }

        return analysisProvider.analyze(tenantId, events);
    }

    private void validateLookbackHours(int lookbackHours) {
        if (lookbackHours < MIN_LOOKBACK_HOURS || lookbackHours > MAX_LOOKBACK_HOURS) {
            throw new IllegalArgumentException(
                    "lookbackHours must be between " + MIN_LOOKBACK_HOURS + " and " + MAX_LOOKBACK_HOURS + "."
            );
        }
    }
}
