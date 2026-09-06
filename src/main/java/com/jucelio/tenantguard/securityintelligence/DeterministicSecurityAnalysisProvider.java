package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditEventResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicSecurityAnalysisProvider implements SecurityAnalysisProvider {

    @Override
    public SecurityAnalysis analyze(String tenantId, List<AuditEventResponse> events) {
        int failedEvents = (int) events.stream()
                .filter(this::isFailure)
                .count();

        SecurityAnalysis.RiskLevel riskLevel = riskLevel(failedEvents);
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
            recommendations.add("Revisar os eventos com falha e correlacioná-los por requestId e traceId.");
        }

        return new SecurityAnalysis(
                tenantId,
                events.size(),
                failedEvents,
                riskLevel,
                List.copyOf(findings),
                List.copyOf(recommendations)
        );
    }

    private boolean isFailure(AuditEventResponse event) {
        return event.outcome() == null || !"SUCCESS".equalsIgnoreCase(event.outcome());
    }

    private SecurityAnalysis.RiskLevel riskLevel(int failedEvents) {
        if (failedEvents >= 5) {
            return SecurityAnalysis.RiskLevel.HIGH;
        }
        if (failedEvents >= 2) {
            return SecurityAnalysis.RiskLevel.MEDIUM;
        }
        return SecurityAnalysis.RiskLevel.LOW;
    }
}
