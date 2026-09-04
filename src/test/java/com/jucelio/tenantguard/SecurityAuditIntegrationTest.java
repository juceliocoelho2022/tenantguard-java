package com.jucelio.tenantguard;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.security.JwtService;
import com.jucelio.tenantguard.security.audit.SecurityEvent;
import com.jucelio.tenantguard.security.audit.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuditIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("tenantguard")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired SecurityEventRepository repository;

    @Test
    void missingAuthenticationShouldBeAuditedAs401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());

        List<SecurityEvent> events = repository.findByEventTypeOrderByCreatedAtDesc("AUTHENTICATION_REQUIRED");
        assertFalse(events.isEmpty());
        assertEquals(401, events.getFirst().getHttpStatus());
        assertEquals("/api/orders", events.getFirst().getPath());
    }

    @Test
    void accessDeniedShouldBeAuditedWithTenantAndUser() throws Exception {
        String token = jwtService.generateToken(new AuthenticatedUser("user-a", "TENANT_A", "USER"));

        mockMvc.perform(get("/api/admin/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        List<SecurityEvent> events = repository.findByEventTypeOrderByCreatedAtDesc("ACCESS_DENIED");
        assertFalse(events.isEmpty());
        SecurityEvent event = events.getFirst();
        assertEquals(403, event.getHttpStatus());
        assertEquals("TENANT_A", event.getTenantId());
        assertEquals("user-a", event.getUsername());
    }

    @Test
    void rateLimitExceededShouldBeAuditedAs429() throws Exception {
        String body = "{\"username\":\"user-a\",\"password\":\"wrong\"}";

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> { request.setRemoteAddr("10.10.10.50"); return request; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr("10.10.10.50"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());

        List<SecurityEvent> events = repository.findByEventTypeOrderByCreatedAtDesc("RATE_LIMIT_EXCEEDED");
        assertFalse(events.isEmpty());
        assertEquals(429, events.getFirst().getHttpStatus());
        assertEquals("10.10.10.50", events.getFirst().getClientIp());
    }
}
