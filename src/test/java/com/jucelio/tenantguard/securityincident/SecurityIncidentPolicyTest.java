package com.jucelio.tenantguard.securityincident;

import com.jucelio.tenantguard.securityintelligence.SecurityAnalysis;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityIncidentPolicyTest {

    private final SecurityIncidentPolicy policy = new SecurityIncidentPolicy();

    @Test
    void shouldNotOpenIncidentForLowRiskAnalysis() {
        SecurityAnalysis analysis = analysis(
                15,
                SecurityAnalysis.RiskLevel.LOW,
                List.of(SecurityAnalysis.SignalCategory.ACCESS_DENIED)
        );

        assertTrue(policy.evaluate(analysis).isEmpty());
    }

    @Test
    void shouldOpenMediumIncidentAtThreshold() {
        SecurityAnalysis analysis = analysis(
                40,
                SecurityAnalysis.RiskLevel.MEDIUM,
                List.of(SecurityAnalysis.SignalCategory.AUTH_FAILURE)
        );

        SecurityIncidentDecision decision = policy.evaluate(analysis).orElseThrow();

        assertEquals(SecurityIncidentSeverity.MEDIUM, decision.severity());
        assertEquals(40, decision.riskScore());
        assertEquals(64, decision.fingerprint().length());
    }

    @Test
    void shouldOpenIncidentForTokenReplayEvenBelowThreshold() {
        SecurityAnalysis analysis = analysis(
                35,
                SecurityAnalysis.RiskLevel.MEDIUM,
                List.of(SecurityAnalysis.SignalCategory.TOKEN_REPLAY)
        );

        SecurityIncidentDecision decision = policy.evaluate(analysis).orElseThrow();

        assertEquals(SecurityIncidentSeverity.MEDIUM, decision.severity());
    }

    @Test
    void shouldClassifyHighAndCriticalSeverityDeterministically() {
        assertEquals(
                SecurityIncidentSeverity.HIGH,
                policy.evaluate(analysis(
                        65,
                        SecurityAnalysis.RiskLevel.HIGH,
                        List.of(SecurityAnalysis.SignalCategory.RATE_LIMIT)
                )).orElseThrow().severity()
        );

        assertEquals(
                SecurityIncidentSeverity.CRITICAL,
                policy.evaluate(analysis(
                        85,
                        SecurityAnalysis.RiskLevel.HIGH,
                        List.of(SecurityAnalysis.SignalCategory.TOKEN_REPLAY)
                )).orElseThrow().severity()
        );
    }

    @Test
    void shouldGenerateSameFingerprintRegardlessOfCategoryOrder() {
        SecurityAnalysis first = analysis(
                60,
                SecurityAnalysis.RiskLevel.HIGH,
                List.of(
                        SecurityAnalysis.SignalCategory.TOKEN_REPLAY,
                        SecurityAnalysis.SignalCategory.ACCESS_DENIED
                )
        );

        SecurityAnalysis second = analysis(
                60,
                SecurityAnalysis.RiskLevel.HIGH,
                List.of(
                        SecurityAnalysis.SignalCategory.ACCESS_DENIED,
                        SecurityAnalysis.SignalCategory.TOKEN_REPLAY
                )
        );

        assertEquals(
                policy.evaluate(first).orElseThrow().fingerprint(),
                policy.evaluate(second).orElseThrow().fingerprint()
        );
    }

    private SecurityAnalysis analysis(
            int riskScore,
            SecurityAnalysis.RiskLevel riskLevel,
            List<SecurityAnalysis.SignalCategory> categories
    ) {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-07T08:00:00-03:00");

        return new SecurityAnalysis(
                "TENANT_A",
                now.minusHours(24),
                now,
                10,
                3,
                riskScore,
                riskLevel,
                categories,
                List.of("finding"),
                List.of("recommendation")
        );
    }
}
