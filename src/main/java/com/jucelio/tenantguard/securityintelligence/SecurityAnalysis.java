package com.jucelio.tenantguard.securityintelligence;

import java.util.List;

public record SecurityAnalysis(
        String tenantId,
        int totalEvents,
        int failedEvents,
        RiskLevel riskLevel,
        List<String> findings,
        List<String> recommendations
) {
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
