package com.jucelio.tenantguard.securityintelligence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityIntelligenceControllerTest {

    @Test
    void shouldDelegateAnalysisToSecurityIntelligenceService() {
        SecurityIntelligenceService service = mock(SecurityIntelligenceService.class);
        SecurityAnalysis expected = new SecurityAnalysis(
                "TENANT_A",
                3,
                1,
                SecurityAnalysis.RiskLevel.LOW,
                List.of("1 failed security-related event detected."),
                List.of("Continue monitoring authentication and authorization events.")
        );
        when(service.analyzeCurrentTenant()).thenReturn(expected);

        SecurityIntelligenceController controller = new SecurityIntelligenceController(service);

        SecurityAnalysis actual = controller.analyze();

        assertSame(expected, actual);
        verify(service).analyzeCurrentTenant();
    }
}
