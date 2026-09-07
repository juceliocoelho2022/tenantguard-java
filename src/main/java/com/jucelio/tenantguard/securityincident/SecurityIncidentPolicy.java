package com.jucelio.tenantguard.securityincident;

import com.jucelio.tenantguard.securityintelligence.SecurityAnalysis;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityIncidentPolicy {

    private static final int INCIDENT_THRESHOLD = 40;

    private final SecurityIncidentFingerprint fingerprint;

    public SecurityIncidentPolicy(SecurityIncidentFingerprint fingerprint) {
        this.fingerprint = fingerprint;
    }

    public Optional<SecurityIncidentDecision> evaluate(SecurityAnalysis analysis) {
        if (analysis == null) {
            throw new IllegalArgumentException("analysis must not be null");
        }

        boolean tokenReplay = analysis.categories().contains(SecurityAnalysis.SignalCategory.TOKEN_REPLAY);
        boolean shouldOpen = tokenReplay || analysis.riskScore() >= INCIDENT_THRESHOLD;

        if (!shouldOpen) {
            return Optional.empty();
        }

        SecurityIncidentSeverity severity = severityFor(analysis.riskScore(), tokenReplay);

        return Optional.of(new SecurityIncidentDecision(
                severity,
                analysis.riskScore(),
                fingerprint.generate(analysis)
        ));
    }

    SecurityIncidentSeverity severityFor(int riskScore, boolean tokenReplay) {
        if (riskScore >= 80) {
            return SecurityIncidentSeverity.CRITICAL;
        }
        if (riskScore >= 60 || (tokenReplay && riskScore >= 50)) {
            return SecurityIncidentSeverity.HIGH;
        }
        if (riskScore >= INCIDENT_THRESHOLD || tokenReplay) {
            return SecurityIncidentSeverity.MEDIUM;
        }
        return SecurityIncidentSeverity.LOW;
    }
}
