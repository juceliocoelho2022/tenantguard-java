package com.jucelio.tenantguard.securityincident;

import com.jucelio.tenantguard.securityintelligence.SecurityAnalysis;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SecurityIncidentPolicy {

    private static final int INCIDENT_THRESHOLD = 40;

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
        String fingerprint = fingerprintFor(analysis);

        return Optional.of(new SecurityIncidentDecision(
                severity,
                analysis.riskScore(),
                fingerprint
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

    String fingerprintFor(SecurityAnalysis analysis) {
        String categories = analysis.categories().stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));

        String source = analysis.tenantId()
                + "|" + analysis.riskLevel().name()
                + "|" + categories;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
