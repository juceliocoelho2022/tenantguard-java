package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditEventResponse;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class DeterministicSecurityAnalysisProvider implements SecurityAnalysisProvider {

    @Override
    public SecurityAnalysis analyze(String tenantId, List<AuditEventResponse> events) {
        List<AuditEventResponse> failed = events.stream()
                .filter(this::isFailure)
                .toList();

        int failedEvents = failed.size();
        int riskScore = Math.min(100, failed.stream().mapToInt(this::scoreEvent).sum());
        SecurityAnalysis.RiskLevel riskLevel = riskLevel(riskScore);
        Set<SecurityAnalysis.SignalCategory> categories = new LinkedHashSet<>();
        failed.forEach(event -> categories.add(classify(event)));

        OffsetDateTime windowStart = events.stream()
                .map(AuditEventResponse::createdAt)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        OffsetDateTime windowEnd = events.stream()
                .map(AuditEventResponse::createdAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<String> findings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (events.isEmpty()) {
            findings.add("Nenhum evento de auditoria disponível para análise.");
            recommendations.add("Gerar tráfego autenticado antes de avaliar o risco do tenant.");
        } else if (failedEvents == 0) {
            findings.add("Nenhum evento com outcome diferente de SUCCESS foi identificado.");
            recommendations.add("Manter o monitoramento contínuo dos eventos de auditoria.");
        } else {
            findings.add("Foram identificados " + failedEvents + " eventos com outcome de falha.");
            findings.add("Risk score determinístico calculado em " + riskScore + "/100.");
            recommendations.add("Revisar os eventos com falha e correlacioná-los por requestId e traceId.");
            if (categories.contains(SecurityAnalysis.SignalCategory.TOKEN_REPLAY)) {
                recommendations.add("Priorizar a investigação de replay de token e revogar sessões relacionadas.");
            }
            if (categories.contains(SecurityAnalysis.SignalCategory.RATE_LIMIT)) {
                recommendations.add("Correlacionar eventos de rate limit por usuário, origem e janela temporal.");
            }
        }

        return new SecurityAnalysis(
                tenantId,
                windowStart,
                windowEnd,
                events.size(),
                failedEvents,
                riskScore,
                riskLevel,
                List.copyOf(categories),
                List.copyOf(findings),
                List.copyOf(recommendations)
        );
    }

    private boolean isFailure(AuditEventResponse event) {
        return event.outcome() == null || !"SUCCESS".equalsIgnoreCase(event.outcome());
    }

    private int scoreEvent(AuditEventResponse event) {
        return switch (classify(event)) {
            case AUTH_FAILURE -> 10;
            case ACCESS_DENIED -> 15;
            case RATE_LIMIT -> 20;
            case TOKEN_REPLAY -> 30;
            case GENERIC_FAILURE -> 10;
        };
    }

    private SecurityAnalysis.SignalCategory classify(AuditEventResponse event) {
        String action = normalize(event.action());
        String outcome = normalize(event.outcome());

        if (action.contains("REPLAY") || action.contains("TOKEN_REUSE")) {
            return SecurityAnalysis.SignalCategory.TOKEN_REPLAY;
        }
        if (action.contains("RATE_LIMIT") || outcome.equals("429") || outcome.contains("RATE_LIMIT")) {
            return SecurityAnalysis.SignalCategory.RATE_LIMIT;
        }
        if (action.contains("ACCESS_DENIED") || action.contains("AUTHORIZATION") || outcome.contains("DENIED") || outcome.equals("403")) {
            return SecurityAnalysis.SignalCategory.ACCESS_DENIED;
        }
        if (action.contains("LOGIN") || action.contains("AUTH")) {
            return SecurityAnalysis.SignalCategory.AUTH_FAILURE;
        }
        return SecurityAnalysis.SignalCategory.GENERIC_FAILURE;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private SecurityAnalysis.RiskLevel riskLevel(int riskScore) {
        if (riskScore >= 50) {
            return SecurityAnalysis.RiskLevel.HIGH;
        }
        if (riskScore >= 20) {
            return SecurityAnalysis.RiskLevel.MEDIUM;
        }
        return SecurityAnalysis.RiskLevel.LOW;
    }
}
