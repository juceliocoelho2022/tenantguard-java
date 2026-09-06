package com.jucelio.tenantguard.securityintelligence;

import java.time.OffsetDateTime;
import java.util.List;

public record SecurityAnalysis(
        String tenantId,
        OffsetDateTime analysisWindowStart,
        OffsetDateTime analysisWindowEnd,
        int totalEvents,
        int failedEvents,
        int riskScore,
        RiskLevel riskLevel,
        List<SignalCategory> categories,
        List<String> findings,
        List<String> recommendations
) {
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum SignalCategory {
        AUTH_FAILURE,
        ACCESS_DENIED,
        RATE_LIMIT,
        TOKEN_REPLAY,
        GENERIC_FAILURE
    }
}
