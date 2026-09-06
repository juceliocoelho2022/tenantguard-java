package com.jucelio.tenantguard.securityintelligence;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
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
                OffsetDateTime.parse("2026-09-06T12:00:00Z"),
                OffsetDateTime.parse("2026-09-06T12:10:00Z"),
                3,
                1,
                10,
                SecurityAnalysis.RiskLevel.LOW,
                List.of(SecurityAnalysis.SignalCategory.GENERIC_FAILURE),
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
