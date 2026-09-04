CREATE TABLE security_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    http_status INTEGER NOT NULL,
    tenant_id VARCHAR(50),
    username VARCHAR(100),
    method VARCHAR(10) NOT NULL,
    path VARCHAR(255) NOT NULL,
    client_ip VARCHAR(64),
    request_id VARCHAR(100),
    trace_id VARCHAR(64),
    details VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_security_events_created_at ON security_events (created_at DESC);
CREATE INDEX idx_security_events_tenant_id ON security_events (tenant_id);
CREATE INDEX idx_security_events_event_type ON security_events (event_type);
CREATE INDEX idx_security_events_username ON security_events (username);
