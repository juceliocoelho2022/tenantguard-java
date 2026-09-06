package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditEventResponse;

import java.util.List;

public interface SecurityAnalysisProvider {

    SecurityAnalysis analyze(String tenantId, List<AuditEventResponse> events);
}
