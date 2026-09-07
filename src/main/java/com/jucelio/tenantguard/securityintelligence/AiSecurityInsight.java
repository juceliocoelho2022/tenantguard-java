package com.jucelio.tenantguard.securityintelligence;

import java.util.List;

public record AiSecurityInsight(
        List<String> findings,
        List<String> recommendations
) {
    public AiSecurityInsight {
        findings = findings == null ? List.of() : List.copyOf(findings);
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
