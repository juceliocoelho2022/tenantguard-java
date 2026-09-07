package com.jucelio.tenantguard.securityintelligence;

import java.time.OffsetDateTime;
import java.util.List;

public record AiSecurityRequest(
        String tenantId,
        int deterministicRiskScore,
        SecurityAnalysis.RiskLevel deterministicRiskLevel,
        List<SecurityAnalysis.SignalCategory> deterministicCategories,
        List<Evidence> evidence
) {
    public record Evidence(
            String username,
            String action,
            String outcome,
            String requestId,
            String traceId,
            String details,
            OffsetDateTime createdAt,
            SecurityEvidence.Source source
    ) {
        public static Evidence from(SecurityEvidence event) {
            return new Evidence(
                    event.username(),
                    event.action(),
                    event.outcome(),
                    event.requestId(),
                    event.traceId(),
                    event.details(),
                    event.createdAt(),
                    event.source()
            );
        }
    }

    public static AiSecurityRequest from(
            SecurityAnalysis deterministic,
            List<SecurityEvidence> events
    ) {
        return new AiSecurityRequest(
                deterministic.tenantId(),
                deterministic.riskScore(),
                deterministic.riskLevel(),
                deterministic.categories(),
                events.stream().map(Evidence::from).toList()
        );
    }
}
