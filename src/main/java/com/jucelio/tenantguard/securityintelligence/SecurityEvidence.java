package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditEventResponse;
import com.jucelio.tenantguard.security.audit.SecurityEventResponse;

import java.time.OffsetDateTime;

public record SecurityEvidence(
        String tenantId,
        String username,
        String action,
        String outcome,
        String requestId,
        String traceId,
        String details,
        OffsetDateTime createdAt,
        Source source
) {
    public enum Source {
        AUDIT,
        SECURITY
    }

    public static SecurityEvidence fromAudit(AuditEventResponse event) {
        return new SecurityEvidence(
                event.tenantId(),
                event.username(),
                event.action(),
                event.outcome(),
                event.requestId(),
                event.traceId(),
                null,
                event.createdAt(),
                Source.AUDIT
        );
    }

    public static SecurityEvidence fromSecurity(SecurityEventResponse event) {
        return new SecurityEvidence(
                event.tenantId(),
                event.username(),
                event.eventType(),
                Integer.toString(event.httpStatus()),
                event.requestId(),
                event.traceId(),
                event.details(),
                event.createdAt(),
                Source.SECURITY
        );
    }
}
