-- Sprint 8: extend tenant isolation to authentication and security data.
-- The tenantguard_app role already exists from V2.

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE refresh_token_sessions TO tenantguard_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE security_events TO tenantguard_app;
GRANT USAGE, SELECT ON SEQUENCE security_events_id_seq TO tenantguard_app;

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
