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
class RbacIntegrationTest {

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
    void missingAuthentication_shouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/admin/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"));
    }

    @Test
    void userRole_shouldBeForbiddenFromAdminEndpoint() throws Exception {
        String token = token("user-a", "TENANT_A", "USER");

        mockMvc.perform(get("/api/admin/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("forbidden"));
    }

    @Test
    void adminRole_shouldAccessAdminEndpoint() throws Exception {
        String token = token("admin-a", "TENANT_A", "ADMIN");

        mockMvc.perform(get("/api/admin/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.message").value("Acesso administrativo autorizado."));
    }

    @Test
    void adminRole_shouldStillRespectTenantIsolation() throws Exception {
        String token = token("admin-a", "TENANT_A", "ADMIN");

        mockMvc.perform(get("/api/orders/3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    private String token(String username, String tenantId, String role) {
        return jwtService.generateToken(
                new AuthenticatedUser(username, tenantId, role)
        );
    }
}
