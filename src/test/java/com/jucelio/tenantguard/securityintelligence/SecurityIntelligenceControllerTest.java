package com.jucelio.tenantguard.securityintelligence;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityIntelligenceControllerTest {

    @Test
    void shouldDelegateAnalysisWithLookbackToSecurityIntelligenceService() {
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
        when(service.analyzeCurrentTenant(24)).thenReturn(expected);

        SecurityIntelligenceController controller = new SecurityIntelligenceController(service);

        SecurityAnalysis actual = controller.analyze(24);

        assertSame(expected, actual);
        verify(service).analyzeCurrentTenant(24);
    }

    @Test
    void shouldTranslateInvalidLookbackToBadRequest() {
        SecurityIntelligenceService service = mock(SecurityIntelligenceService.class);
        when(service.analyzeCurrentTenant(0))
                .thenThrow(new IllegalArgumentException("lookbackHours must be between 1 and 168."));

        SecurityIntelligenceController controller = new SecurityIntelligenceController(service);

        assertThatThrownBy(() -> controller.analyze(0))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("lookbackHours");
    }
}
