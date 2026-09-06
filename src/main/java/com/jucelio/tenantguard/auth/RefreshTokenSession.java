package com.jucelio.tenantguard.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "refresh_token_sessions")
public class RefreshTokenSession {

    public static final String REVOCATION_REASON_ROTATED = "ROTATED";
    public static final String REVOCATION_REASON_LOGOUT = "LOGOUT";

    @Id
    @Column(length = 100, nullable = false)
    private String jti;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revocation_reason", length = 20)
    private String revocationReason;

    protected RefreshTokenSession() {
    }

    public RefreshTokenSession(
            String jti,
            String username,
            String tenantId,
            String role,
            OffsetDateTime expiresAt,
            OffsetDateTime createdAt) {
        this.jti = jti;
        this.username = username;
        this.tenantId = tenantId;
        this.role = role;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public String getJti() {
        return jti;
    }

    public String getUsername() {
        return username;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRole() {
        return role;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void revoke(OffsetDateTime when, String reason) {
        this.revokedAt = when;
        this.revocationReason = reason;
    }

    public boolean isActive(OffsetDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public boolean wasRotated() {
        return REVOCATION_REASON_ROTATED.equals(revocationReason);
    }
}
