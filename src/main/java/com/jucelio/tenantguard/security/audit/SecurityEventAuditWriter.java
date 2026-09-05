package com.jucelio.tenantguard.security.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class SecurityEventAuditWriter {

    private final JdbcTemplate jdbcTemplate;

    public SecurityEventAuditWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(
            String eventType,
            int httpStatus,
            String username,
            String method,
            String path,
            String clientIp,
            String requestId,
            String traceId,
            String details,
            OffsetDateTime createdAt) {

        jdbcTemplate.update("""
                INSERT INTO security_events (
                    event_type,
                    http_status,
                    tenant_id,
                    username,
                    method,
                    path,
                    client_ip,
                    request_id,
                    trace_id,
                    details,
                    created_at
                ) VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                eventType,
                httpStatus,
                username,
                method,
                path,
                clientIp,
                requestId,
                traceId,
                details,
                createdAt
        );
    }
}
