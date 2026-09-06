package com.jucelio.tenantguard.securityintelligence;

import com.jucelio.tenantguard.audit.AuditEventResponse;
import com.jucelio.tenantguard.audit.AuditService;
import com.jucelio.tenantguard.security.audit.SecurityEventResponse;
import com.jucelio.tenantguard.security.audit.SecurityEventService;
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
        SecurityEventService securityEventService = mock(SecurityEventService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, securityEventService, provider);

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents(24, SecurityIntelligenceService.EVENT_CAP)).thenReturn(List.of(
                auditEvent(1L, "TENANT_A", "ORDER_READ", "SUCCESS", "2026-09-06T12:00:00Z"),
                auditEvent(2L, "TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:05:00Z")
        ));
        when(securityEventService.findCurrentTenantEvents(24, SecurityIntelligenceService.EVENT_CAP)).thenReturn(List.of());

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
        verify(securityEventService).findCurrentTenantEvents(24, 500);
    }

    @Test
    void shouldIncludeTenantSecurityEventsInRiskScore() {
        AuditService auditService = mock(AuditService.class);
        SecurityEventService securityEventService = mock(SecurityEventService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, securityEventService, provider);

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents(24, 500)).thenReturn(List.of());
        when(securityEventService.findCurrentTenantEvents(24, 500)).thenReturn(List.of(
                securityEvent(1L, "TENANT_A", "ACCESS_DENIED", 403, "2026-09-06T12:05:00Z")
        ));

        SecurityAnalysis result = service.analyzeCurrentTenant(24);

        assertThat(result.totalEvents()).isEqualTo(1);
        assertThat(result.failedEvents()).isEqualTo(1);
        assertThat(result.riskScore()).isEqualTo(15);
        assertThat(result.categories()).containsExactly(SecurityAnalysis.SignalCategory.ACCESS_DENIED);
    }

    @Test
    void shouldUseDefaultLookbackAndEventCap() {
        AuditService auditService = mock(AuditService.class);
        SecurityEventService securityEventService = mock(SecurityEventService.class);
        SecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();
        SecurityIntelligenceService service = new SecurityIntelligenceService(auditService, securityEventService, provider);

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents(24, 500)).thenReturn(List.of());
        when(securityEventService.findCurrentTenantEvents(24, 500)).thenReturn(List.of());

        service.analyzeCurrentTenant();

        verify(auditService).findCurrentTenantEvents(24, 500);
        verify(securityEventService).findCurrentTenantEvents(24, 500);
    }

    @Test
    void shouldRejectLookbackBelowMinimum() {
        SecurityIntelligenceService service = new SecurityIntelligenceService(
                mock(AuditService.class),
                mock(SecurityEventService.class),
                new DeterministicSecurityAnalysisProvider()
        );

        assertThatThrownBy(() -> service.analyzeCurrentTenant(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 168");
    }

    @Test
    void shouldRejectLookbackAboveMaximum() {
        SecurityIntelligenceService service = new SecurityIntelligenceService(
                mock(AuditService.class),
                mock(SecurityEventService.class),
                new DeterministicSecurityAnalysisProvider()
        );

        assertThatThrownBy(() -> service.analyzeCurrentTenant(169))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 168");
    }

    @Test
    void shouldRejectEventsFromAnotherTenant() {
        AuditService auditService = mock(AuditService.class);
        SecurityEventService securityEventService = mock(SecurityEventService.class);
        SecurityIntelligenceService service = new SecurityIntelligenceService(
                auditService,
                securityEventService,
                new DeterministicSecurityAnalysisProvider()
        );

        TenantContext.setTenant("TENANT_A");
        when(auditService.findCurrentTenantEvents(24, 500)).thenReturn(List.of(
                auditEvent(1L, "TENANT_A", "ORDER_READ", "SUCCESS", "2026-09-06T12:00:00Z"),
                auditEvent(2L, "TENANT_B", "ORDER_READ", "FAILURE", "2026-09-06T12:05:00Z")
        ));
        when(securityEventService.findCurrentTenantEvents(24, 500)).thenReturn(List.of());

        assertThatThrownBy(() -> service.analyzeCurrentTenant(24))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another tenant");
    }

    @Test
    void shouldClassifyMediumRiskFromTwoFailures() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                evidence("TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:00:00Z"),
                evidence("TENANT_A", "ORDER_READ", "DENIED", "2026-09-06T12:01:00Z")
        ));

        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.MEDIUM);
        assertThat(result.failedEvents()).isEqualTo(2);
        assertThat(result.riskScore()).isEqualTo(25);
    }

    @Test
    void shouldClassifyHighRiskFromFiveFailures() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                evidence("TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:00:00Z"),
                evidence("TENANT_A", "ORDER_READ", "FAILURE", "2026-09-06T12:01:00Z"),
                evidence("TENANT_A", "ORDER_READ", "DENIED", "2026-09-06T12:02:00Z"),
                evidence("TENANT_A", "ORDER_READ", "ERROR", "2026-09-06T12:03:00Z"),
                evidence("TENANT_A", "ORDER_READ", null, "2026-09-06T12:04:00Z")
        ));

        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.HIGH);
        assertThat(result.failedEvents()).isEqualTo(5);
        assertThat(result.riskScore()).isEqualTo(55);
    }

    @Test
    void shouldWeightReplayAndRateLimitSignals() {
        DeterministicSecurityAnalysisProvider provider = new DeterministicSecurityAnalysisProvider();

        SecurityAnalysis result = provider.analyze("TENANT_A", List.of(
                evidence("TENANT_A", "TOKEN_REPLAY", "401", "2026-09-06T12:00:00Z"),
                evidence("TENANT_A", "RATE_LIMIT_EXCEEDED", "429", "2026-09-06T12:01:00Z")
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
                evidence("TENANT_A", "TOKEN_REPLAY", "401", "2026-09-06T12:00:00Z"),
                evidence("TENANT_A", "TOKEN_REPLAY", "401", "2026-09-06T12:01:00Z"),
                evidence("TENANT_A", "TOKEN_REPLAY", "401", "2026-09-06T12:02:00Z"),
                evidence("TENANT_A", "TOKEN_REPLAY", "401", "2026-09-06T12:03:00Z")
        ));

        assertThat(result.riskScore()).isEqualTo(100);
        assertThat(result.riskLevel()).isEqualTo(SecurityAnalysis.RiskLevel.HIGH);
    }

    private SecurityEvidence evidence(String tenantId, String action, String outcome, String createdAt) {
        return new SecurityEvidence(
                tenantId,
                "user-a",
                action,
                outcome,
                "request",
                "trace",
                null,
                OffsetDateTime.parse(createdAt),
                SecurityEvidence.Source.AUDIT
        );
    }

    private AuditEventResponse auditEvent(Long id, String tenantId, String action, String outcome, String createdAt) {
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

    private SecurityEventResponse securityEvent(Long id, String tenantId, String eventType, int status, String createdAt) {
        return new SecurityEventResponse(
                id,
                eventType,
                status,
                tenantId,
                "user-a",
                "GET",
                "/api/admin/security-intelligence",
                "127.0.0.1",
                "request-" + id,
                "trace-" + id,
                "test",
                OffsetDateTime.parse(createdAt)
        );
    }
}
