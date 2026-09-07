CREATE TABLE security_incidents (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(120) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    risk_score INTEGER NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolution_note VARCHAR(1000),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT ck_security_incidents_severity
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_security_incidents_status
        CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_security_incidents_tenant_created
    ON security_incidents (tenant_id, created_at DESC);

CREATE INDEX idx_security_incidents_tenant_status
    ON security_incidents (tenant_id, status);

CREATE INDEX idx_security_incidents_tenant_fingerprint
    ON security_incidents (tenant_id, fingerprint);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE security_incidents TO tenantguard_app;

ALTER TABLE security_incidents ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_incidents FORCE ROW LEVEL SECURITY;

CREATE POLICY security_incidents_tenant_isolation
ON security_incidents
FOR ALL
TO tenantguard_app
USING (
    tenant_id = current_setting('app.current_tenant', true)
)
WITH CHECK (
    tenant_id = current_setting('app.current_tenant', true)
);
