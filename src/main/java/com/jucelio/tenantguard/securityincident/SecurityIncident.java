package com.jucelio.tenantguard.securityincident;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class SecurityIncident {

    private final UUID id;
    private final String tenantId;
    private final SecurityIncidentSeverity severity;
    private final int riskScore;
    private final String fingerprint;
    private final OffsetDateTime createdAt;
    private SecurityIncidentStatus status;
    private OffsetDateTime updatedAt;
    private String resolutionNote;

    private SecurityIncident(
            UUID id,
            String tenantId,
            SecurityIncidentSeverity severity,
            int riskScore,
            String fingerprint,
            SecurityIncidentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String resolutionNote
    ) {
        this.id = Objects.requireNonNull(id);
        this.tenantId = requireText(tenantId, "tenantId");
        this.severity = Objects.requireNonNull(severity);
        this.riskScore = validateRiskScore(riskScore);
        this.fingerprint = requireText(fingerprint, "fingerprint");
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.resolutionNote = resolutionNote;
    }

    public static SecurityIncident open(
            String tenantId,
            SecurityIncidentSeverity severity,
            int riskScore,
            String fingerprint,
            OffsetDateTime now
    ) {
        return new SecurityIncident(
                UUID.randomUUID(),
                tenantId,
                severity,
                riskScore,
                fingerprint,
                SecurityIncidentStatus.OPEN,
                now,
                now,
                null
        );
    }

    public void startInvestigation(OffsetDateTime now) {
        requireStatus(SecurityIncidentStatus.OPEN);
        status = SecurityIncidentStatus.INVESTIGATING;
        updatedAt = Objects.requireNonNull(now);
    }

    public void resolve(String note, OffsetDateTime now) {
        if (status != SecurityIncidentStatus.OPEN && status != SecurityIncidentStatus.INVESTIGATING) {
            throw new IllegalStateException("Only open or investigating incidents can be resolved");
        }

        resolutionNote = requireText(note, "resolutionNote");
        status = SecurityIncidentStatus.RESOLVED;
        updatedAt = Objects.requireNonNull(now);
    }

    public void dismiss(String note, OffsetDateTime now) {
        if (status != SecurityIncidentStatus.OPEN && status != SecurityIncidentStatus.INVESTIGATING) {
            throw new IllegalStateException("Only open or investigating incidents can be dismissed");
        }

        resolutionNote = requireText(note, "resolutionNote");
        status = SecurityIncidentStatus.DISMISSED;
        updatedAt = Objects.requireNonNull(now);
    }

    private void requireStatus(SecurityIncidentStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected incident status " + expected + " but was " + status);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static int validateRiskScore(int value) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("riskScore must be between 0 and 100");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public SecurityIncidentSeverity getSeverity() {
        return severity;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public SecurityIncidentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }
}
