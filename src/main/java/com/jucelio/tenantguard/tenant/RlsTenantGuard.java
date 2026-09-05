package com.jucelio.tenantguard.tenant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RlsTenantGuard {

    private final JdbcTemplate jdbcTemplate;

    public RlsTenantGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void applyCurrentTenant() {
        applyTenant(TenantContext.getTenant());
    }

    public void applyTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant não pode ser vazio para aplicar RLS.");
        }

        jdbcTemplate.execute("SET LOCAL ROLE tenantguard_app");
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.current_tenant', ?, true)",
                String.class,
                tenantId
        );
    }

    public void applyAuditWriter() {
        jdbcTemplate.execute("SET LOCAL ROLE tenantguard_audit");
    }
}
