CREATE TABLE refresh_token_sessions (
    jti VARCHAR(100) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_token_sessions_tenant_id
    ON refresh_token_sessions (tenant_id);

CREATE INDEX idx_refresh_token_sessions_username
    ON refresh_token_sessions (username);

CREATE INDEX idx_refresh_token_sessions_expires_at
    ON refresh_token_sessions (expires_at);
