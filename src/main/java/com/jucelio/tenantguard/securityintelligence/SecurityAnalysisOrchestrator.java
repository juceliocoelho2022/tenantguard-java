package com.jucelio.tenantguard.securityintelligence;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

@Component
@Primary
public class SecurityAnalysisOrchestrator implements SecurityAnalysisProvider {

    private final DeterministicSecurityAnalysisProvider deterministicProvider;
    private final ObjectProvider<AiSecurityClient> aiClientProvider;

    public SecurityAnalysisOrchestrator(
            DeterministicSecurityAnalysisProvider deterministicProvider,
            ObjectProvider<AiSecurityClient> aiClientProvider
    ) {
        this.deterministicProvider = deterministicProvider;
        this.aiClientProvider = aiClientProvider;
    }

    @Override
    public SecurityAnalysis analyze(String tenantId, List<SecurityEvidence> events) {
        SecurityAnalysis deterministic = deterministicProvider.analyze(tenantId, events);
        AiSecurityClient aiClient = aiClientProvider.getIfAvailable();

        if (aiClient == null) {
            return deterministic;
        }

        try {
            AiSecurityInsight insight = aiClient.analyze(AiSecurityRequest.from(deterministic, events));
            if (insight == null) {
                return deterministic;
            }

            return new SecurityAnalysis(
                    deterministic.tenantId(),
                    deterministic.analysisWindowStart(),
                    deterministic.analysisWindowEnd(),
                    deterministic.totalEvents(),
                    deterministic.failedEvents(),
                    deterministic.riskScore(),
                    deterministic.riskLevel(),
                    deterministic.categories(),
                    merge(deterministic.findings(), insight.findings()),
                    merge(deterministic.recommendations(), insight.recommendations())
            );
        } catch (RuntimeException ignored) {
            return deterministic;
        }
    }

    private List<String> merge(List<String> baseline, List<String> aiValues) {
        LinkedHashSet<String> values = new LinkedHashSet<>(baseline);
        aiValues.stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(values::add);
        return List.copyOf(values);
    }
}
