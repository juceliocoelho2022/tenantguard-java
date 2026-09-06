package com.jucelio.tenantguard.securityintelligence;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;

@Component
@Primary
public class SecurityAnalysisOrchestrator implements SecurityAnalysisProvider {

    private final DeterministicSecurityAnalysisProvider deterministicProvider;
    private final ObjectProvider<AiSecurityClient> aiClientProvider;
    private final MeterRegistry meterRegistry;
    private final int maxAiEvents;
    private final Counter attempts;
    private final Counter successes;
    private final Counter failures;
    private final Counter fallbacks;
    private final Timer latency;

    public SecurityAnalysisOrchestrator(
            DeterministicSecurityAnalysisProvider deterministicProvider,
            ObjectProvider<AiSecurityClient> aiClientProvider,
            MeterRegistry meterRegistry,
            @Value("${app.security-intelligence.ai.max-events:100}") int maxAiEvents
    ) {
        if (maxAiEvents < 1 || maxAiEvents > 500) {
            throw new IllegalArgumentException("AI max-events must be between 1 and 500");
        }

        this.deterministicProvider = deterministicProvider;
        this.aiClientProvider = aiClientProvider;
        this.meterRegistry = meterRegistry;
        this.maxAiEvents = maxAiEvents;
        this.attempts = meterRegistry.counter("tenantguard.security.intelligence.ai.attempts");
        this.successes = meterRegistry.counter("tenantguard.security.intelligence.ai.successes");
        this.failures = meterRegistry.counter("tenantguard.security.intelligence.ai.failures");
        this.fallbacks = meterRegistry.counter("tenantguard.security.intelligence.ai.fallbacks");
        this.latency = meterRegistry.timer("tenantguard.security.intelligence.ai.latency");
    }

    @Override
    public SecurityAnalysis analyze(String tenantId, List<SecurityEvidence> events) {
        SecurityAnalysis deterministic = deterministicProvider.analyze(tenantId, events);
        AiSecurityClient aiClient = aiClientProvider.getIfAvailable();

        if (aiClient == null) {
            return deterministic;
        }

        List<SecurityEvidence> boundedEvents = events.stream()
                .limit(maxAiEvents)
                .toList();

        attempts.increment();
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            AiSecurityInsight insight = aiClient.analyze(AiSecurityRequest.from(deterministic, boundedEvents));
            if (insight == null) {
                failures.increment();
                fallbacks.increment();
                return deterministic;
            }

            successes.increment();
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
            failures.increment();
            fallbacks.increment();
            return deterministic;
        } finally {
            sample.stop(latency);
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
