package com.jucelio.tenantguard.securityincident;

public record SecurityIncidentDecision(
        SecurityIncidentSeverity severity,
        int riskScore,
        String fingerprint
) {
}
