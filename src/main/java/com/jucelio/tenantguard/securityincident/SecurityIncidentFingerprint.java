package com.jucelio.tenantguard.securityincident;

import com.jucelio.tenantguard.securityintelligence.SecurityAnalysis;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class SecurityIncidentFingerprint {

    public String generate(SecurityAnalysis analysis) {
        if (analysis == null) {
            throw new IllegalArgumentException("analysis must not be null");
        }

        String categories = analysis.categories().stream()
                .map(Enum::name)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(","));

        String source = analysis.tenantId() + "|" + categories;

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
