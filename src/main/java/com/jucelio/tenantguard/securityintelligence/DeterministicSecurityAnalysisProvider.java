package com.jucelio.tenantguard.securityintelligence;

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
    public SecurityAnalysis analyze(String tenantId, List<SecurityEvidence> events) {
        List<SecurityEvidence> failed = events.stream()
                .filter(this::isFailure)
                .toList();

        int failedEvents = failed.size();
        int riskScore = Math.min(100, failed.stream().mapToInt(this::scoreEvent).sum());
        SecurityAnalysis.RiskLevel riskLevel = riskLevel(riskScore);
        Set<SecurityAnalysis.SignalCategory> categories = new LinkedHashSet<>();
        failed.forEach(event -> categories.add(classify(event)));

        OffsetDateTime windowStart = events.stream()
                .map(SecurityEvidence::createdAt)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        OffsetDateTime windowEnd = events.stream()
                .map(SecurityEvidence::createdAt)
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<String> findings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (events.isEmpty()) {
            findings.add("Nenhum evento de segurança ou auditoria disponível para análise.");
            recommendations.add("Gerar tráfego autenticado antes de avaliar o risco do tenant.");
        } else if (failedEvents == 0) {
            findings.add("Nenhum evento de falha foi identificado no período analisado.");
            recommendations.add("Manter o monitoramento contínuo dos eventos de segurança e auditoria.");
        } else {
            findings.add("Foram identificados " + failedEvents + " eventos de falha no período analisado.");
            findings.add("Risk score determinístico calculado em " + riskScore + "/100.");
            recommendations.add("Revisar os eventos com falha e correlacioná-los por requestId e traceId.");
            if (categories.contains(SecurityAnalysis.SignalCategory.TOKEN_REPLAY)) {
                recommendations.add("Priorizar a investigação de replay de token e revogar sessões relacionadas.");
            }
            if (categories.contains(SecurityAnalysis.SignalCategory.RATE_LIMIT)) {
                recommendations.add("Correlacionar eventos de rate limit por usuário, origem e janela temporal.");
            }
            if (categories.contains(SecurityAnalysis.SignalCategory.ACCESS_DENIED)) {
                recommendations.add("Revisar tentativas de acesso negado e confirmar o princípio de menor privilégio.");
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

    private boolean isFailure(SecurityEvidence event) {
        if (event.source() == SecurityEvidence.Source.SECURITY) {
            return parseStatus(event.outcome()) >= 400;
        }
        return event.outcome() == null || !"SUCCESS".equalsIgnoreCase(event.outcome());
    }

    private int scoreEvent(SecurityEvidence event) {
        return switch (classify(event)) {
            case AUTH_FAILURE -> 10;
            case ACCESS_DENIED -> 15;
            case RATE_LIMIT -> 20;
            case TOKEN_REPLAY -> 30;
            case GENERIC_FAILURE -> 10;
        };
    }

    private SecurityAnalysis.SignalCategory classify(SecurityEvidence event) {
        String action = normalize(event.action());
        String outcome = normalize(event.outcome());

        if (action.contains("TOKEN_REPLAY") || action.contains("TOKEN_REUSE") || action.contains("REPLAY_DETECTED")) {
            return SecurityAnalysis.SignalCategory.TOKEN_REPLAY;
        }
        if (action.contains("RATE_LIMIT") || outcome.equals("429") || outcome.contains("RATE_LIMIT")) {
            return SecurityAnalysis.SignalCategory.RATE_LIMIT;
        }
        if (action.contains("ACCESS_DENIED") || action.contains("AUTHORIZATION") || outcome.contains("DENIED") || outcome.equals("403")) {
            return SecurityAnalysis.SignalCategory.ACCESS_DENIED;
        }
        if (action.contains("LOGIN") || action.contains("AUTH") || action.contains("REFRESH_FAILED")) {
            return SecurityAnalysis.SignalCategory.AUTH_FAILURE;
        }
        return SecurityAnalysis.SignalCategory.GENERIC_FAILURE;
    }

    private int parseStatus(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return 500;
        }
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
