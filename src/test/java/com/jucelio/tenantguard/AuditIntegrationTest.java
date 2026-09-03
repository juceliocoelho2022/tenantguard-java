package com.jucelio.tenantguard;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuditIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("tenantguard")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtService jwtService;

    @Test
    void adminShouldSeeOnlyAuditEventsFromItsTenant() throws Exception {
        String userAToken = token("user-a", "TENANT_A", "USER");
        String userBToken = token("user-b", "TENANT_B", "USER");
        String adminAToken = token("admin-a", "TENANT_A", "ADMIN");

        mockMvc.perform(get("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userBToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value("TENANT_A"))
                .andExpect(jsonPath("$[0].username").value("user-a"))
                .andExpect(jsonPath("$[0].action").value("ORDER_LIST"));
    }

    @Test
    void regularUserShouldNotAccessAuditEndpoint() throws Exception {
        String userAToken = token("user-a", "TENANT_A", "USER");

        mockMvc.perform(get("/api/admin/audit-events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAToken))
                .andExpect(status().isForbidden());
    }

    private String token(String username, String tenantId, String role) {
        return jwtService.generateToken(new AuthenticatedUser(username, tenantId, role));
    }
}
