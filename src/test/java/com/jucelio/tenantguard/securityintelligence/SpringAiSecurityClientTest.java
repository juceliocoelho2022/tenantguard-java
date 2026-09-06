package com.jucelio.tenantguard.securityintelligence;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiSecurityClientTest {

    @Test
    void shouldBuildPromptWithDeterministicContext() {
        SpringAiSecurityClient client = client();
        AiSecurityRequest request = new AiSecurityRequest(
                "TENANT_A",
                45,
                SecurityAnalysis.RiskLevel.MEDIUM,
                List.of(
                        SecurityAnalysis.SignalCategory.TOKEN_REPLAY,
                        SecurityAnalysis.SignalCategory.ACCESS_DENIED
                ),
                List.of(new AiSecurityRequest.Evidence(
                        "user-a",
                        "TOKEN_REPLAY",
                        "401",
                        "request-1",
                        "trace-1",
                        "Previously rotated refresh token was reused",
                        OffsetDateTime.parse("2026-09-06T20:46:30Z"),
                        SecurityEvidence.Source.SECURITY
                ))
        );

        String prompt = client.buildUserPrompt(request);

        assertTrue(prompt.contains("Tenant: TENANT_A"));
        assertTrue(prompt.contains("Deterministic risk score: 45/100"));
        assertTrue(prompt.contains("TOKEN_REPLAY"));
        assertTrue(prompt.contains("user=user-a"));
    }

    @Test
    void shouldRedactSensitiveEvidenceDetails() {
        SpringAiSecurityClient client = client();
        String secret = "Authorization: Bearer super-secret-token";
        AiSecurityRequest request = new AiSecurityRequest(
                "TENANT_A",
                30,
                SecurityAnalysis.RiskLevel.MEDIUM,
                List.of(SecurityAnalysis.SignalCategory.TOKEN_REPLAY),
                List.of(new AiSecurityRequest.Evidence(
                        "user-a",
                        "TOKEN_REPLAY",
                        "401",
                        "request-1",
                        "trace-1",
                        secret,
                        OffsetDateTime.parse("2026-09-06T20:46:30Z"),
                        SecurityEvidence.Source.SECURITY
                ))
        );

        String prompt = client.buildUserPrompt(request);

        assertTrue(prompt.contains("details=[REDACTED]"));
        assertFalse(prompt.contains("super-secret-token"));
    }

    private SpringAiSecurityClient client() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        return new SpringAiSecurityClient(builder);
    }
}
