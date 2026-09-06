package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditEventResponse;
import com.jucelio.tenantguard.audit.AuditService;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityIntelligenceServiceTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void shouldAnalyzeOnlyCurrentTenantEvents() {
        AuditService auditService = mock(AuditService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, provider);

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents()).thenReturn(List.of(
                event(1L, "TENANT_A", "SUCCESS"),
                event(2L, "TENANT_A", "FAILURE")
        ));

        SecurityAnalysis result = service.analyzeCurrentTenant();

        assertThat(result.tenantId()).isEqualTo("TENANT_A");
        assertThat(result.totalEvents()).isEqualTo(2);
        assertThat(result.failedEvents()).isEqualTo(1);
        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.LOW);
    }

    @Test
    void shouldRejectEventsFromAnotherTenant() {
        AuditService auditService = mock(AuditService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, provider);

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents()).thenReturn(List.of(
                event(1L, "TENANT_A", "SUCCESS"),
                event(2L, "TENANT_B", "FAILURE")
        ));

        assertThatThrownBy(service::analyzeCurrentTenant)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another tenant");
    }

    @Test
    void shouldClassifyMediumRiskFromTwoFailures() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                event(1L, "TENANT_A", "FAILURE"),
                event(2L, "TENANT_A", "DENIED")
        ));

        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.MEDIUM);
        assertThat(result.failedEvents()).isEqualTo(2);
    }

    @Test
    void shouldClassifyHighRiskFromFiveFailures() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                event(1L, "TENANT_A", "FAILURE"),
                event(2L, "TENANT_A", "FAILURE"),
                event(3L, "TENANT_A", "DENIED"),
                event(4L, "TENANT_A", "ERROR"),
                event(5L, "TENANT_A", null)
        ));

        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.HIGH);
        assertThat(result.failedEvents()).isEqualTo(5);
    }

    private AuditEventResponse event(Long id, String tenantId, String outcome) {
        return new AuditEventResponse(
                id,
                tenantId,
                "user-a",
                "ORDER_READ",
                "ORDER",
                "1",
                outcome,
                "request-" + id,
                "trace-" + id,
                OffsetDateTime.parse("2026-09-06T12:00:00Z")
        );
    }
}
