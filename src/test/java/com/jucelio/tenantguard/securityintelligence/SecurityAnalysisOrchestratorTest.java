package com.jucelio.tenantguard.securityintelligence;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;

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
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        SecurityAnalysisOrchestrator orchestrator =
                new SecurityAnalysisOrchestrator(deterministicProvider, provider, registry, 100);

        SecurityAnalysis result = orchestrator.analyze("TENANT_A", List.of(tokenReplayEvent()));

        assertEquals(30, result.riskScore());
        assertEquals(SecurityAnalysis.RiskLevel.MEDIUM, result.riskLevel());
        assertEquals(List.of(SecurityAnalysis.SignalCategory.TOKEN_REPLAY), result.categories());
        assertTrue(result.findings().stream().anyMatch(value -> value.contains("30/100")));
        assertEquals(0.0, registry.counter("tenantguard.security.intelligence.ai.attempts").count());
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
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        SecurityAnalysisOrchestrator orchestrator =
                new SecurityAnalysisOrchestrator(deterministicProvider, provider, registry, 100);

        SecurityAnalysis result = orchestrator.analyze("TENANT_A", List.of(tokenReplayEvent()));

        assertEquals(30, result.riskScore());
        assertEquals(SecurityAnalysis.RiskLevel.MEDIUM, result.riskLevel());
        assertEquals(List.of(SecurityAnalysis.SignalCategory.TOKEN_REPLAY), result.categories());
        assertTrue(result.findings().contains("AI: possível reutilização coordenada de credenciais."));
        assertTrue(result.recommendations().contains(
                "AI: correlacionar a sessão com eventos recentes do mesmo usuário."));
        assertEquals(1.0, registry.counter("tenantguard.security.intelligence.ai.attempts").count());
        assertEquals(1.0, registry.counter("tenantguard.security.intelligence.ai.successes").count());
        assertEquals(0.0, registry.counter("tenantguard.security.intelligence.ai.fallbacks").count());

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
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        SecurityAnalysisOrchestrator orchestrator =
                new SecurityAnalysisOrchestrator(deterministicProvider, provider, registry, 100);

        SecurityAnalysis result = orchestrator.analyze("TENANT_A", List.of(tokenReplayEvent()));

        assertEquals(30, result.riskScore());
        assertEquals(SecurityAnalysis.RiskLevel.MEDIUM, result.riskLevel());
        assertFalse(result.findings().isEmpty());
        assertFalse(result.recommendations().isEmpty());
        assertEquals(1.0, registry.counter("tenantguard.security.intelligence.ai.attempts").count());
        assertEquals(1.0, registry.counter("tenantguard.security.intelligence.ai.failures").count());
        assertEquals(1.0, registry.counter("tenantguard.security.intelligence.ai.fallbacks").count());
    }

    @Test
    void shouldLimitEvidenceSentToAiClient() {
        AiSecurityClient aiClient = mock(AiSecurityClient.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AiSecurityClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(aiClient);
        when(aiClient.analyze(any())).thenReturn(new AiSecurityInsight(List.of(), List.of()));
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        List<SecurityEvidence> events = IntStream.range(0, 120)
                .mapToObj(index -> new SecurityEvidence(
                        "TENANT_A",
                        "user-a",
                        "ACCESS_DENIED",
                        "403",
                        "request-" + index,
                        "trace-" + index,
                        "Denied",
                        OffsetDateTime.parse("2026-09-06T20:46:30Z").minusSeconds(index),
                        SecurityEvidence.Source.SECURITY
                ))
                .toList();

        SecurityAnalysisOrchestrator orchestrator =
                new SecurityAnalysisOrchestrator(deterministicProvider, provider, registry, 100);

        orchestrator.analyze("TENANT_A", events);

        verify(aiClient).analyze(argThat(request -> request.evidence().size() == 100));
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
