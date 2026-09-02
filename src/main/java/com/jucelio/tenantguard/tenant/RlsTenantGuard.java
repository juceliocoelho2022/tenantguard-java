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
        String tenantId = TenantContext.getTenant();

        jdbcTemplate.execute("SET LOCAL ROLE tenantguard_app");
        jdbcTemplate.queryForObject(
                "SELECT set_config('app.current_tenant', ?, true)",
                String.class,
                tenantId
        );
    }
}
