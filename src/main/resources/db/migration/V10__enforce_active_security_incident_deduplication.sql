-- Prevent concurrent requests from creating more than one active incident
-- for the same tenant and deterministic fingerprint.
CREATE UNIQUE INDEX uk_security_incidents_active_fingerprint
    ON security_incidents (tenant_id, fingerprint)
    WHERE status IN ('OPEN', 'INVESTIGATING');
