package com.jucelio.tenantguard.securityincident;

import com.jucelio.tenantguard.securityintelligence.SecurityAnalysis;
import com.jucelio.tenantguard.tenant.RlsTenantGuard;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityIncidentServiceTest {

    private SecurityIncidentPolicy policy;
    private SecurityIncidentRepository repository;
    private RlsTenantGuard rlsTenantGuard;
    private SecurityIncidentAuditService auditService;
    private SecurityIncidentMetrics metrics;
    private SecurityIncidentService service;

    @BeforeEach
    void setUp() {
        policy = mock(SecurityIncidentPolicy.class);
        repository = mock(SecurityIncidentRepository.class);
        rlsTenantGuard = mock(RlsTenantGuard.class);
        auditService = mock(SecurityIncidentAuditService.class);
        metrics = mock(SecurityIncidentMetrics.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC);
        service = new SecurityIncidentService(policy, repository, rlsTenantGuard, auditService, metrics, clock);
        TenantContext.setTenant("TENANT_A");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldNotCreateIncidentWhenPolicyDoesNotTrigger() {
        SecurityAnalysis analysis = analysis("TENANT_A", 15);
        when(policy.evaluate(analysis)).thenReturn(Optional.empty());

        Optional<SecurityIncident> result = service.evaluateAndOpen(analysis);

        assertTrue(result.isEmpty());
        verify(rlsTenantGuard).applyCurrentTenant();
        verify(repository, never()).save(any());
        verify(auditService, never()).opened(any());
        verify(metrics, never()).opened(any());
    }

    @Test
    void shouldReturnExistingActiveIncidentWithoutCreatingDuplicate() {
        SecurityAnalysis analysis = analysis("TENANT_A", 60);
        SecurityIncidentDecision decision =
                new SecurityIncidentDecision(SecurityIncidentSeverity.HIGH, 60, "abc123");
        SecurityIncident existing = SecurityIncident.open(
                "TENANT_A",
                SecurityIncidentSeverity.HIGH,
                60,
                "abc123",
                OffsetDateTime.parse("2026-09-07T11:00:00Z")
        );

        when(policy.evaluate(analysis)).thenReturn(Optional.of(decision));
        when(repository.findFirstByTenantIdAndFingerprintAndStatusIn(
                "TENANT_A",
                "abc123",
                List.of(SecurityIncidentStatus.OPEN, SecurityIncidentStatus.INVESTIGATING)
        )).thenReturn(Optional.of(existing));

        Optional<SecurityIncident> result = service.evaluateAndOpen(analysis);

        assertTrue(result.isPresent());
        assertEquals(existing.getId(), result.get().getId());
        verify(repository, never()).save(any());
        verify(auditService, never()).opened(any());
        verify(metrics).deduplicated(existing);
    }

    @Test
    void shouldCreateIncidentWhenPolicyTriggersAndNoActiveDuplicateExists() {
        SecurityAnalysis analysis = analysis("TENANT_A", 60);
        SecurityIncidentDecision decision =
                new SecurityIncidentDecision(SecurityIncidentSeverity.HIGH, 60, "abc123");

        when(policy.evaluate(analysis)).thenReturn(Optional.of(decision));
        when(repository.findFirstByTenantIdAndFingerprintAndStatusIn(
                "TENANT_A",
                "abc123",
                List.of(SecurityIncidentStatus.OPEN, SecurityIncidentStatus.INVESTIGATING)
        )).thenReturn(Optional.empty());
        when(repository.save(any(SecurityIncident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<SecurityIncident> result = service.evaluateAndOpen(analysis);

        assertTrue(result.isPresent());
        assertEquals("TENANT_A", result.get().getTenantId());
        assertEquals(SecurityIncidentStatus.OPEN, result.get().getStatus());
        assertEquals(SecurityIncidentSeverity.HIGH, result.get().getSeverity());
        assertEquals(OffsetDateTime.parse("2026-09-07T12:00:00Z"), result.get().getCreatedAt());
        verify(repository).save(any(SecurityIncident.class));
        verify(auditService).opened(result.get());
        verify(metrics).opened(result.get());
    }

    @Test
    void shouldRejectAnalysisFromAnotherTenant() {
        SecurityAnalysis analysis = analysis("TENANT_B", 60);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.evaluateAndOpen(analysis)
        );

        assertEquals(
                "Security analysis tenant does not match the authenticated tenant",
                exception.getMessage()
        );
        verify(rlsTenantGuard, never()).applyCurrentTenant();
        verify(repository, never()).save(any());
        verify(auditService, never()).opened(any());
        verify(metrics, never()).opened(any());
    }

    @Test
    void shouldStartInvestigationForCurrentTenantIncident() {
        SecurityIncident incident = incident();
        when(repository.findByIdAndTenantId(incident.getId(), "TENANT_A"))
                .thenReturn(Optional.of(incident));
        when(repository.save(incident)).thenReturn(incident);

        SecurityIncident result = service.startInvestigation(incident.getId());

        assertEquals(SecurityIncidentStatus.INVESTIGATING, result.getStatus());
        assertEquals(OffsetDateTime.parse("2026-09-07T12:00:00Z"), result.getUpdatedAt());
        verify(rlsTenantGuard).applyCurrentTenant();
        verify(repository).save(incident);
        verify(auditService).investigationStarted(incident);
        verify(metrics).investigationStarted(incident);
    }

    @Test
    void shouldResolveCurrentTenantIncident() {
        SecurityIncident incident = incident();
        when(repository.findByIdAndTenantId(incident.getId(), "TENANT_A"))
                .thenReturn(Optional.of(incident));
        when(repository.save(incident)).thenReturn(incident);

        SecurityIncident result = service.resolve(incident.getId(), "Mitigated after investigation");

        assertEquals(SecurityIncidentStatus.RESOLVED, result.getStatus());
        assertEquals("Mitigated after investigation", result.getResolutionNote());
        assertEquals(OffsetDateTime.parse("2026-09-07T12:00:00Z"), result.getResolvedAt());
        verify(auditService).resolved(incident);
        verify(metrics).resolved(incident);
    }

    @Test
    void shouldDismissCurrentTenantIncident() {
        SecurityIncident incident = incident();
        when(repository.findByIdAndTenantId(incident.getId(), "TENANT_A"))
                .thenReturn(Optional.of(incident));
        when(repository.save(incident)).thenReturn(incident);

        SecurityIncident result = service.dismiss(incident.getId(), "Confirmed false positive");

        assertEquals(SecurityIncidentStatus.DISMISSED, result.getStatus());
        assertEquals("Confirmed false positive", result.getResolutionNote());
        assertEquals(OffsetDateTime.parse("2026-09-07T12:00:00Z"), result.getResolvedAt());
        verify(auditService).dismissed(incident);
        verify(metrics).dismissed(incident);
    }

    @Test
    void shouldReturnNotFoundForIncidentOutsideCurrentTenant() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndTenantId(id, "TENANT_A"))
                .thenReturn(Optional.empty());

        assertThrows(SecurityIncidentNotFoundException.class, () -> service.startInvestigation(id));

        verify(rlsTenantGuard).applyCurrentTenant();
        verify(repository, never()).save(any());
        verify(auditService, never()).investigationStarted(any());
        verify(metrics, never()).investigationStarted(any());
    }

    private SecurityIncident incident() {
        return SecurityIncident.open(
                "TENANT_A",
                SecurityIncidentSeverity.HIGH,
                60,
                "abc123",
                OffsetDateTime.parse("2026-09-07T11:00:00Z")
        );
    }

    private SecurityAnalysis analysis(String tenantId, int riskScore) {
        return new SecurityAnalysis(
                tenantId,
                null,
                null,
                3,
                2,
                riskScore,
                riskScore >= 60 ? SecurityAnalysis.RiskLevel.HIGH : SecurityAnalysis.RiskLevel.LOW,
                List.of(SecurityAnalysis.SignalCategory.ACCESS_DENIED),
                List.of("finding"),
                List.of("recommendation")
        );
    }
}
