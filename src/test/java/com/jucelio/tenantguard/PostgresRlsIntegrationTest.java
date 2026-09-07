package com.jucelio.tenantguard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
class PostgresRlsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("tenantguard")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void rls_shouldFilterRowsEvenWithoutTenantPredicate() {
        applyTenant("TENANT_A");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders",
                Integer.class
        );

        assertEquals(2, count);
    }

    @Test
    @Transactional
    void rls_shouldRejectCrossTenantInsert() {
        applyTenant("TENANT_A");

        assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO orders (description, tenant_id) VALUES (?, ?)",
                        "Tentativa indevida",
                        "TENANT_B"
                )
        );
    }

    @Test
    @Transactional
    void refreshSessions_shouldBeIsolatedByTenant() {
        jdbcTemplate.update(
                "INSERT INTO refresh_token_sessions (jti, username, tenant_id, role, expires_at) VALUES (?, ?, ?, ?, ?)",
                "jti-a", "alice", "TENANT_A", "USER", OffsetDateTime.now().plusHours(1)
        );
        jdbcTemplate.update(
                "INSERT INTO refresh_token_sessions (jti, username, tenant_id, role, expires_at) VALUES (?, ?, ?, ?, ?)",
                "jti-b", "bob", "TENANT_B", "USER", OffsetDateTime.now().plusHours(1)
        );

        applyTenant("TENANT_A");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_token_sessions",
                Integer.class
        );

        assertEquals(1, count);
    }

    @Test
    @Transactional
    void securityEvents_shouldBeIsolatedByTenant() {
        jdbcTemplate.update(
                "INSERT INTO security_events (event_type, http_status, tenant_id, method, path) VALUES (?, ?, ?, ?, ?)",
                "AUTH_FAILURE", 401, "TENANT_A", "POST", "/api/auth/login"
        );
        jdbcTemplate.update(
                "INSERT INTO security_events (event_type, http_status, tenant_id, method, path) VALUES (?, ?, ?, ?, ?)",
                "AUTH_FAILURE", 401, "TENANT_B", "POST", "/api/auth/login"
        );

        applyTenant("TENANT_A");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_events",
                Integer.class
        );

        assertEquals(1, count);
    }

    @Test
    @Transactional
    void securityIncidents_shouldBeIsolatedByTenant() {
        insertSecurityIncident("TENANT_A", "fingerprint-a", "OPEN");
        insertSecurityIncident("TENANT_B", "fingerprint-b", "OPEN");

        applyTenant("TENANT_A");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_incidents",
                Integer.class
        );

        assertEquals(1, count);
    }

    @Test
    @Transactional
    void securityIncidents_shouldRejectCrossTenantInsert() {
        applyTenant("TENANT_A");

        assertThrows(DataAccessException.class, () ->
                jdbcTemplate.update(
                        """
                        INSERT INTO security_incidents (
                            id, tenant_id, severity, risk_score, fingerprint, status,
                            created_at, updated_at, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        "TENANT_B",
                        "HIGH",
                        70,
                        "cross-tenant-fingerprint",
                        "OPEN",
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        0L
                )
        );
    }

    @Test
    @Transactional
    void securityIncidents_shouldRejectDuplicateActiveFingerprintPerTenant() {
        insertSecurityIncident("TENANT_A", "same-fingerprint", "OPEN");

        assertThrows(DataAccessException.class, () ->
                insertSecurityIncident("TENANT_A", "same-fingerprint", "INVESTIGATING")
        );
    }

    @Test
    @Transactional
    void auditWriter_shouldInsertPreAuthenticationEventWithoutReadPermission() {
        jdbcTemplate.execute("SET LOCAL ROLE tenantguard_audit");

        int inserted = jdbcTemplate.update(
                "INSERT INTO security_events (event_type, http_status, tenant_id, method, path) VALUES (?, ?, ?, ?, ?)",
                "AUTH_FAILURE", 401, null, "POST", "/api/auth/login"
        );

        assertEquals(1, inserted);
        assertThrows(DataAccessException.class, () ->
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM security_events", Integer.class)
        );
    }

    private void insertSecurityIncident(String tenantId, String fingerprint, String status) {
        jdbcTemplate.update(
                """
                INSERT INTO security_incidents (
                    id, tenant_id, severity, risk_score, fingerprint, status,
                    created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                tenantId,
                "HIGH",
                70,
                fingerprint,
                status,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                0L
        );
    }

    private void applyTenant(String tenantId) {
        jdbcTemplate.execute("SET LOCAL ROLE tenantguard_app");
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.current_tenant', ?, true)",
                String.class,
                tenantId
        );
    }
}
