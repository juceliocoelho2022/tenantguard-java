CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    username VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    outcome VARCHAR(30) NOT NULL,
    request_id VARCHAR(100),
    trace_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_events_tenant_id ON audit_events (tenant_id);
CREATE INDEX idx_audit_events_created_at ON audit_events (created_at DESC);
CREATE INDEX idx_audit_events_trace_id ON audit_events (trace_id);

GRANT SELECT, INSERT ON TABLE audit_events TO tenantguard_app;
GRANT USAGE, SELECT ON SEQUENCE audit_events_id_seq TO tenantguard_app;

ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS audit_events_tenant_isolation ON audit_events;

CREATE POLICY audit_events_tenant_isolation
ON audit_events
FOR ALL
TO tenantguard_app
USING (
    tenant_id = current_setting('app.current_tenant', true)
)
WITH CHECK (
    tenant_id = current_setting('app.current_tenant', true)
);
