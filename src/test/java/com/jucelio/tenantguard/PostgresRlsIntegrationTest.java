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

    private void applyTenant(String tenantId) {
        jdbcTemplate.execute("SET LOCAL ROLE tenantguard_app");
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.current_tenant', ?, true)",
                String.class,
                tenantId
        );
    }
}
