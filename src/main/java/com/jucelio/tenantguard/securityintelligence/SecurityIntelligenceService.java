package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditEventResponse;
import com.jucelio.tenantguard.audit.AuditService;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityIntelligenceService {

    private final AuditService auditService;
    private final SecurityAnalysisProvider analysisProvider;

    public SecurityIntelligenceService(
            AuditService auditService,
            SecurityAnalysisProvider analysisProvider
    ) {
        this.auditService = auditService;
        this.analysisProvider = analysisProvider;
    }

    public SecurityAnalysis analyzeCurrentTenant() {
        String tenantId = TenantContext.getTenant();
        List<AuditEventResponse> events = auditService.findCurrentTenantEvents();

        boolean containsAnotherTenant = events.stream()
                .anyMatch(event -> !tenantId.equals(event.tenantId()));

        if (containsAnotherTenant) {
            throw new IllegalStateException("Security intelligence received events from another tenant.");
        }

        return analysisProvider.analyze(tenantId, events);
    }
}
