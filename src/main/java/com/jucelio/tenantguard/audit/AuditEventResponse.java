package com.jucelio.tenantguard.audit;

import java.time.OffsetDateTime;

public record AuditEventResponse(
        Long id,
        String tenantId,
        String username,
        String action,
        String resourceType,
        String resourceId,
        String outcome,
        String requestId,
        String traceId,
        OffsetDateTime createdAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getTenantId(),
                event.getUsername(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getOutcome(),
                event.getRequestId(),
                event.getTraceId(),
                event.getCreatedAt()
        );
    }
}
