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
import static org.mockito.Mockito.verify;
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
        when(auditService.findCurrentTenantEvents(24, SecurityIntelligenceService.EVENT_CAP)).thenReturn(List.of(
                event(1L, "TENANT_A", "ORDER_READ", "SUCCESS", "2026-09-06T12:00:00Z"),
                event(2L, "TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:05:00Z")
        ));

        SecurityAnalysis result = service.analyzeCurrentTenant(24);

        assertThat(result.tenantId()).isEqualTo("TENANT_A");
        assertThat(result.totalEvents()).isEqualTo(2);
        assertThat(result.failedEvents()).isEqualTo(1);
        assertThat(result.riskScore()).isEqualTo(10);
        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.LOW);
        assertThat(result.categories()).containsExactly(SecurityAnalysis.SignalCategory.GENERIC_FAILURE);
        assertThat(result.analysisWindowStart()).isEqualTo(OffsetDateTime.parse("2026-09-06T12:00:00Z"));
        assertThat(result.analysisWindowEnd()).isEqualTo(OffsetDateTime.parse("2026-09-06T12:05:00Z"));
        verify(auditService).findCurrentTenantEvents(24, 500);
    }

    @Test
    void shouldUseDefaultLookbackAndEventCap() {
        AuditService auditService = mock(AuditService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, provider);

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents(
                SecurityIntelligenceService.DEFAULT_LOOKBACK_HOURS,
                SecurityIntelligenceService.EVENT_CAP
        )).thenReturn(List.of());

        service.analyzeCurrentTenant();

        verify(auditService).findCurrentTenantEvents(24, 500);
    }

    @Test
    void shouldRejectLookbackBelowMinimum() {
        AuditService auditService = mock(AuditService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, provider);

        assertThatThrownBy(() -> service.analyzeCurrentTenant(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 168");
    }

    @Test
    void shouldRejectLookbackAboveMaximum() {
        AuditService auditService = mock(AuditService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, provider);

        assertThatThrownBy(() -> service.analyzeCurrentTenant(169))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 168");
    }

    @Test
    void shouldRejectEventsFromAnotherTenant() {
        AuditService auditService = mock(AuditService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, provider);

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents(24, SecurityIntelligenceService.EVENT_CAP)).thenReturn(List.of(
                event(1L, "TENANT_A", "ORDER_READ", "SUCCESS", "2026-09-06T12:00:00Z"),
                event(2L, "TENANT_B", "ORDER_READ", "FAILURE", "2026-09-06T12:05:00Z")
        ));

        assertThatThrownBy(() -> service.analyzeCurrentTenant(24))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another tenant");
    }

    @Test
    void shouldClassifyMediumRiskFromTwoFailures() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                event(1L, "TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:00:00Z"),
                event(2L, "TENANT_A", "ORDER_READ", "DENIED", "2026-09-06T12:01:00Z")
        ));

        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.MEDIUM);
        assertThat(result.failedEvents()).isEqualTo(2);
        assertThat(result.riskScore()).isEqualTo(25);
    }

    @Test
    void shouldClassifyHighRiskFromFiveFailures() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                event(1L, "TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:00:00Z"),
                event(2L, "TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:01:00Z"),
                event(3L, "TENANT_A", "ORDER_READ", "DENIED", "2026-09-06T12:02:00Z"),
                event(4L, "TENANT_A", "ORDER_READ", "ERROR", "2026-09-06T12:03:00Z"),
                event(5L, "TENANT_A", "ORDER_READ", null, "2026-09-06T12:04:00Z")
        ));

        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.HIGH);
        assertThat(result.failedEvents()).isEqualTo(5);
        assertThat(result.riskScore()).isEqualTo(55);
    }

    @Test
    void shouldWeightReplayAndRateLimitSignals() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                event(1L, "TENANT_A", "REFRESH_TOKEN_REPLAY", "FAILURE", "2026-09-06T12:00:00Z"),
                event(2L, "TENANT_A", "AUTH_RATE_LIMIT", "429", "2026-09-06T12:01:00Z")
        ));

        assertThat(result.riskScore()).isEqualTo(50);
        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.HIGH);
        assertThat(result.categories()).containsExactly(
                SecurityAnalysis.SignalCategory.TOKEN_REPLAY,
                SecurityAnalysis.SignalCategory.RATE_LIMIT
        );
        assertThat(result.recommendations())
                .anyMatch(value -> value.contains("replay"))
                .anyMatch(value -> value.contains("rate limit"));
    }

    @Test
    void shouldCapRiskScoreAtOneHundred() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                event(1L, "TENANT_A", "REFRESH_TOKEN_REPLAY", "FAILURE", "2026-09-06T12:00:00Z"),
                event(2L, "TENANT_A", "REFRESH_TOKEN_REPLAY", "FAILURE", "2026-09-06T12:01:00Z"),
                event(3L, "TENANT_A", "REFRESH_TOKEN_REPLAY", "FAILURE", "2026-09-06T12:02:00Z"),
                event(4L, "TENANT_A", "REFRESH_TOKEN_REPLAY", "FAILURE", "2026-09-06T12:03:00Z")
        ));

        assertThat(result.riskScore()).isEqualTo(100);
        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.HIGH);
    }

    private AuditEventResponse event(Long id, String tenantId, String action, String outcome, String createdAt) {
        return new AuditEventResponse(
                id,
                tenantId,
                "user-a",
                action,
                "ORDER",
                "1",
                outcome,
                "request-" + id,
                "trace-" + id,
                OffsetDateTime.parse(createdAt)
        );
    }
}
