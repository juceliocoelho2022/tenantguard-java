package com.jucelio.tenantguard.securityincident;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SecurityIncidentResponse(
        UUID id,
        SecurityIncidentSeverity severity,
        int riskScore,
        String fingerprint,
        SecurityIncidentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime resolvedAt,
        String resolutionNote,
        long version
) {
    public static SecurityIncidentResponse from(SecurityIncident incident) {
        return new SecurityIncidentResponse(
                incident.getId(),
                incident.getSeverity(),
                incident.getRiskScore(),
                incident.getFingerprint(),
                incident.getStatus(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getResolvedAt(),
                incident.getResolutionNote(),
                incident.getVersion()
        );
    }
}
