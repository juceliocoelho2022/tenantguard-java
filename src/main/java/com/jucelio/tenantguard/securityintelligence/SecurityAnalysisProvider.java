package com.jucelio.tenantguard.securityintelligence;

import java.util.List;

public interface SecurityAnalysisProvider {

    SecurityAnalysis analyze(String tenantId, List<SecurityEvidence> events);
}
