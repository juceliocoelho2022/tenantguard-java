package com.jucelio.tenantguard.securityintelligence;

public interface AiSecurityClient {

    AiSecurityInsight analyze(AiSecurityRequest request);
}
