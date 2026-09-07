package com.jucelio.tenantguard;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIncidentApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("tenantguard")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private static final UUID TENANT_A_INCIDENT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B_INCIDENT = UUID.fromString("20000000-0000-0000-0000-000000000001");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedIncidents() {
        jdbcTemplate.update("DELETE FROM security_incidents");
        insertIncident(TENANT_A_INCIDENT, "TENANT_A", "OPEN", "a".repeat(64));
        insertIncident(TENANT_B_INCIDENT, "TENANT_B", "OPEN", "b".repeat(64));
    }

    @Test
    void missingAuthentication_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/security-incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userRole_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/security-incidents")
                        .header(HttpHeaders.AUTHORIZATION, bearer("user-a", "TENANT_A", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_shouldListOnlyCurrentTenantIncidents() throws Exception {
        mockMvc.perform(get("/api/admin/security-incidents")
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin-a", "TENANT_A", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(TENANT_A_INCIDENT.toString()))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"))
                .andExpect(jsonPath("$[0].tenantId").doesNotExist());
    }

    @Test
    void crossTenantIncident_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/security-incidents/{id}", TENANT_B_INCIDENT)
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin-a", "TENANT_A", "ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void admin_shouldMoveIncidentToInvestigation() throws Exception {
        mockMvc.perform(patch("/api/admin/security-incidents/{id}/investigate", TENANT_A_INCIDENT)
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin-a", "TENANT_A", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"));
    }

    @Test
    void invalidLifecycleTransition_shouldReturnConflict() throws Exception {
        String admin = bearer("admin-a", "TENANT_A", "ADMIN");

        mockMvc.perform(patch("/api/admin/security-incidents/{id}/resolve", TENANT_A_INCIDENT)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Mitigated\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/security-incidents/{id}/investigate", TENANT_A_INCIDENT)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict());
    }

    @Test
    void blankResolutionNote_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch("/api/admin/security-incidents/{id}/resolve", TENANT_A_INCIDENT)
                        .header(HttpHeaders.AUTHORIZATION, bearer("admin-a", "TENANT_A", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    private void insertIncident(UUID id, String tenantId, String status, String fingerprint) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO security_incidents " +
                        "(id, tenant_id, severity, risk_score, fingerprint, status, created_at, updated_at, version) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, tenantId, "HIGH", 65, fingerprint, status, now, now, 0L
        );
    }

    private String bearer(String username, String tenantId, String role) {
        return "Bearer " + jwtService.generateToken(new AuthenticatedUser(username, tenantId, role));
    }
}
