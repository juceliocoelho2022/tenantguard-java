ALTER TABLE refresh_token_sessions
    ADD COLUMN revocation_reason VARCHAR(20);
