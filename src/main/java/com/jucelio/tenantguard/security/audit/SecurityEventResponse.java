package com.jucelio.tenantguard.security.audit;

import java.time.OffsetDateTime;

public record SecurityEventResponse(
        Long id,
        String eventType,
        int httpStatus,
        String tenantId,
        String username,
        String method,
        String path,
        String clientIp,
        String requestId,
        String traceId,
        String details,
        OffsetDateTime createdAt
) {
    public static SecurityEventResponse from(SecurityEvent event) {
        return new SecurityEventResponse(
                event.getId(),
                event.getEventType(),
                event.getHttpStatus(),
                event.getTenantId(),
                event.getUsername(),
                event.getMethod(),
                event.getPath(),
                event.getClientIp(),
                event.getRequestId(),
                event.getTraceId(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }
}
