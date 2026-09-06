package com.jucelio.tenantguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jucelio.tenantguard.security.audit.SecurityEvent;
import com.jucelio.tenantguard.security.audit.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshTokenIntegrationTest {

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
    ObjectMapper objectMapper;

    @Autowired
    SecurityEventRepository securityEventRepository;

    @Test
    void loginShouldIssueAccessAndRefreshTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user-a","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tenantId").value("TENANT_A"));
    }

    @Test
    void refreshShouldRotateTokenAndAuditReplayWithTenant() throws Exception {
        String firstRefreshToken = loginAndGetRefreshToken();

        String responseBody = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondRefreshToken = objectMapper.readTree(responseBody)
                .get("refreshToken")
                .asText();

        assertNotEquals(firstRefreshToken, secondRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(firstRefreshToken)))
                .andExpect(status().isUnauthorized());

        List<SecurityEvent> events = securityEventRepository.findByEventTypeOrderByCreatedAtDesc("TOKEN_REPLAY");
        assertFalse(events.isEmpty());
        SecurityEvent replay = events.getFirst();
        assertEquals(401, replay.getHttpStatus());
        assertEquals("TENANT_A", replay.getTenantId());
        assertEquals("user-a", replay.getUsername());
        assertEquals("/api/auth/refresh", replay.getPath());
    }

    @Test
    void logoutShouldRevokeRefreshTokenWithoutClassifyingItAsReplay() throws Exception {
        int replayEventsBefore = securityEventRepository.findByEventTypeOrderByCreatedAtDesc("TOKEN_REPLAY").size();
        String refreshToken = loginAndGetRefreshToken();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized());

        int replayEventsAfter = securityEventRepository.findByEventTypeOrderByCreatedAtDesc("TOKEN_REPLAY").size();
        assertEquals(replayEventsBefore, replayEventsAfter);
    }

    private String loginAndGetRefreshToken() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user-a","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.get("refreshToken").asText();
    }

    private String refreshBody(String refreshToken) throws Exception {
        return objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("refreshToken", refreshToken)
        );
    }
}
