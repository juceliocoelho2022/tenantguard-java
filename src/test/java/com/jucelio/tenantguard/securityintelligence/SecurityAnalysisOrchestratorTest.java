package com.jucelio.tenantguard.securityintelligence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityAnalysisOrchestratorTest {

    private final DeterministicSecurityAnalysisProvider deterministicProvider =
            new DeterministicSecurityAnalysisProvider();

    @Test
    void shouldReturnDeterministicAnalysisWhenAiClientIsUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<AiSecurityClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SecurityAnalysisOrchestrator orchestrator =
                new SecurityAnalysisOrchestrator(deterministicProvider, provider);

        SecurityAnalysis result = orchestrator.analyze("TENANT_A", List.of(tokenReplayEvent()));

        assertEquals(30, result.riskScore());
        assertEquals(SecurityAnalysis.RiskLevel.MEDIUM, result.riskLevel());
        assertEquals(List.of(SecurityAnalysis.SignalCategory.TOKEN_REPLAY), result.categories());
        assertTrue(result.findings().stream().anyMatch(value -> value.contains("30/100")));
    }

    @Test
    void shouldEnrichNarrativeWithoutChangingDeterministicRisk() {
        AiSecurityClient aiClient = mock(AiSecurityClient.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AiSecurityClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(aiClient);
        when(aiClient.analyze(any())).thenReturn(new AiSecurityInsight(
                List.of("AI: possível reutilização coordenada de credenciais."),
                List.of("AI: correlacionar a sessão com eventos recentes do mesmo usuário.")
        ));

        SecurityAnalysisOrchestrator orchestrator =
                new SecurityAnalysisOrchestrator(deterministicProvider, provider);

        SecurityAnalysis result = orchestrator.analyze("TENANT_A", List.of(tokenReplayEvent()));

        assertEquals(30, result.riskScore());
        assertEquals(SecurityAnalysis.RiskLevel.MEDIUM, result.riskLevel());
        assertEquals(List.of(SecurityAnalysis.SignalCategory.TOKEN_REPLAY), result.categories());
        assertTrue(result.findings().contains("AI: possível reutilização coordenada de credenciais."));
        assertTrue(result.recommendations().contains(
                "AI: correlacionar a sessão com eventos recentes do mesmo usuário."));

        verify(aiClient).analyze(argThat(request ->
                request.tenantId().equals("TENANT_A")
                        && request.deterministicRiskScore() == 30
                        && request.evidence().size() == 1
        ));
    }

    @Test
    void shouldFallBackToDeterministicAnalysisWhenAiClientFails() {
        AiSecurityClient aiClient = mock(AiSecurityClient.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AiSecurityClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(aiClient);
        when(aiClient.analyze(any())).thenThrow(new IllegalStateException("provider unavailable"));

        SecurityAnalysisOrchestrator orchestrator =
                new SecurityAnalysisOrchestrator(deterministicProvider, provider);

        SecurityAnalysis result = orchestrator.analyze("TENANT_A", List.of(tokenReplayEvent()));

        assertEquals(30, result.riskScore());
        assertEquals(SecurityAnalysis.RiskLevel.MEDIUM, result.riskLevel());
        assertFalse(result.findings().isEmpty());
        assertFalse(result.recommendations().isEmpty());
    }

    private SecurityEvidence tokenReplayEvent() {
        return new SecurityEvidence(
                "TENANT_A",
                "user-a",
                "TOKEN_REPLAY",
                "401",
                "request-1",
                "trace-1",
                "Previously rotated refresh token was reused",
                OffsetDateTime.parse("2026-09-06T20:46:30Z"),
                SecurityEvidence.Source.SECURITY
        );
    }
}
