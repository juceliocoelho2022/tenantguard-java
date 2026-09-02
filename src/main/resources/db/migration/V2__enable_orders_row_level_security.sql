DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'tenantguard_app'
    ) THEN
        CREATE ROLE tenantguard_app NOLOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA public TO tenantguard_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE orders TO tenantguard_app;
GRANT USAGE, SELECT ON SEQUENCE orders_id_seq TO tenantguard_app;

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS orders_tenant_isolation ON orders;

CREATE POLICY orders_tenant_isolation
ON orders
FOR ALL
TO tenantguard_app
USING (
    tenant_id = current_setting('app.current_tenant', true)
)
WITH CHECK (
    tenant_id = current_setting('app.current_tenant', true)
);
