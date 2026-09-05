package com.jucelio.tenantguard.security.audit;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "http_status", nullable = false)
    private int httpStatus;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(length = 500)
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SecurityEvent() {
    }

    public SecurityEvent(String eventType, int httpStatus, String tenantId, String username,
                         String method, String path, String clientIp, String requestId,
                         String traceId, String details, OffsetDateTime createdAt) {
        this.eventType = eventType;
        this.httpStatus = httpStatus;
        this.tenantId = tenantId;
        this.username = username;
        this.method = method;
        this.path = path;
        this.clientIp = clientIp;
        this.requestId = requestId;
        this.traceId = traceId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public int getHttpStatus() { return httpStatus; }
    public String getTenantId() { return tenantId; }
    public String getUsername() { return username; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getClientIp() { return clientIp; }
    public String getRequestId() { return requestId; }
    public String getTraceId() { return traceId; }
    public String getDetails() { return details; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
