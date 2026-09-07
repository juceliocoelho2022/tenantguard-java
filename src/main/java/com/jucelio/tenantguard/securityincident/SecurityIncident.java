package com.jucelio.tenantguard.securityincident;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "security_incidents")
public class SecurityIncident {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SecurityIncidentSeverity severity;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SecurityIncidentStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Version
    @Column(nullable = false)
    private long version;

    protected SecurityIncident() {
    }

    private SecurityIncident(
            UUID id,
            String tenantId,
            SecurityIncidentSeverity severity,
            int riskScore,
            String fingerprint,
            SecurityIncidentStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime resolvedAt,
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
        this.resolvedAt = resolvedAt;
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
                null,
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

        OffsetDateTime timestamp = Objects.requireNonNull(now);
        resolutionNote = requireText(note, "resolutionNote");
        status = SecurityIncidentStatus.RESOLVED;
        updatedAt = timestamp;
        resolvedAt = timestamp;
    }

    public void dismiss(String note, OffsetDateTime now) {
        if (status != SecurityIncidentStatus.OPEN && status != SecurityIncidentStatus.INVESTIGATING) {
            throw new IllegalStateException("Only open or investigating incidents can be dismissed");
        }

        OffsetDateTime timestamp = Objects.requireNonNull(now);
        resolutionNote = requireText(note, "resolutionNote");
        status = SecurityIncidentStatus.DISMISSED;
        updatedAt = timestamp;
        resolvedAt = timestamp;
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

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public long getVersion() {
        return version;
    }
}
