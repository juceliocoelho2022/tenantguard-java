-- Sprint 8: extend tenant isolation to authentication and security data.
-- tenantguard_app is the tenant-scoped runtime role created by V2.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'tenantguard_audit'
    ) THEN
        CREATE ROLE tenantguard_audit NOLOGIN;
    END IF;
END
$$;

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE refresh_token_sessions TO tenantguard_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE security_events TO tenantguard_app;
GRANT USAGE, SELECT ON SEQUENCE security_events_id_seq TO tenantguard_app;

-- The audit role is deliberately write-only. It supports pre-authentication
-- security events without granting cross-tenant reads.
GRANT INSERT ON TABLE security_events TO tenantguard_audit;
GRANT USAGE, SELECT ON SEQUENCE security_events_id_seq TO tenantguard_audit;

ALTER TABLE refresh_token_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_token_sessions FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS refresh_token_sessions_tenant_isolation ON refresh_token_sessions;

CREATE POLICY refresh_token_sessions_tenant_isolation
ON refresh_token_sessions
FOR ALL
TO tenantguard_app
USING (
    tenant_id = current_setting('app.current_tenant', true)
)
WITH CHECK (
    tenant_id = current_setting('app.current_tenant', true)
);

ALTER TABLE security_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_events FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS security_events_tenant_isolation ON security_events;
DROP POLICY IF EXISTS security_events_audit_insert ON security_events;

CREATE POLICY security_events_tenant_isolation
ON security_events
FOR ALL
TO tenantguard_app
USING (
    tenant_id IS NOT NULL
    AND tenant_id = current_setting('app.current_tenant', true)
)
WITH CHECK (
    tenant_id IS NOT NULL
    AND tenant_id = current_setting('app.current_tenant', true)
);

CREATE POLICY security_events_audit_insert
ON security_events
FOR INSERT
TO tenantguard_audit
WITH CHECK (true);
