package com.jucelio.tenantguard.securityincident;

import com.jucelio.tenantguard.securityintelligence.SecurityAnalysis;
import com.jucelio.tenantguard.tenant.RlsTenantGuard;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityIncidentService {

    private static final List<SecurityIncidentStatus> ACTIVE_STATUSES =
            List.of(SecurityIncidentStatus.OPEN, SecurityIncidentStatus.INVESTIGATING);

    private final SecurityIncidentPolicy policy;
    private final SecurityIncidentRepository repository;
    private final RlsTenantGuard rlsTenantGuard;
    private final SecurityIncidentAuditService auditService;
    private final SecurityIncidentMetrics metrics;
    private final Clock clock;

    @Autowired
    public SecurityIncidentService(
            SecurityIncidentPolicy policy,
            SecurityIncidentRepository repository,
            RlsTenantGuard rlsTenantGuard,
            SecurityIncidentAuditService auditService,
            SecurityIncidentMetrics metrics
    ) {
        this(policy, repository, rlsTenantGuard, auditService, metrics, Clock.systemUTC());
    }

    SecurityIncidentService(
            SecurityIncidentPolicy policy,
            SecurityIncidentRepository repository,
            RlsTenantGuard rlsTenantGuard,
            SecurityIncidentAuditService auditService,
            SecurityIncidentMetrics metrics,
            Clock clock
    ) {
        this.policy = policy;
        this.repository = repository;
        this.rlsTenantGuard = rlsTenantGuard;
        this.auditService = auditService;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Optional<SecurityIncident> evaluateAndOpen(SecurityAnalysis analysis) {
        String tenantId = TenantContext.getTenant();
        ensureAnalysisTenant(analysis, tenantId);
        rlsTenantGuard.applyCurrentTenant();

        Optional<SecurityIncidentDecision> decision = policy.evaluate(analysis);
        if (decision.isEmpty()) {
            return Optional.empty();
        }

        SecurityIncidentDecision incidentDecision = decision.get();

        Optional<SecurityIncident> activeIncident =
                repository.findFirstByTenantIdAndFingerprintAndStatusIn(
                        tenantId,
                        incidentDecision.fingerprint(),
                        ACTIVE_STATUSES
                );

        if (activeIncident.isPresent()) {
            metrics.deduplicated(activeIncident.get());
            return activeIncident;
        }

        SecurityIncident incident = SecurityIncident.open(
                tenantId,
                incidentDecision.severity(),
                incidentDecision.riskScore(),
                incidentDecision.fingerprint(),
                OffsetDateTime.now(clock)
        );

        SecurityIncident saved = repository.save(incident);
        auditService.opened(saved);
        metrics.opened(saved);
        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public List<SecurityIncident> findAll() {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<SecurityIncident> findById(UUID id) {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();
        return repository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public SecurityIncident startInvestigation(UUID id) {
        SecurityIncident incident = requireCurrentTenantIncident(id);
        incident.startInvestigation(OffsetDateTime.now(clock));
        SecurityIncident saved = repository.save(incident);
        auditService.investigationStarted(saved);
        metrics.investigationStarted(saved);
        return saved;
    }

    @Transactional
    public SecurityIncident resolve(UUID id, String note) {
        SecurityIncident incident = requireCurrentTenantIncident(id);
        incident.resolve(note, OffsetDateTime.now(clock));
        SecurityIncident saved = repository.save(incident);
        auditService.resolved(saved);
        metrics.resolved(saved);
        return saved;
    }

    @Transactional
    public SecurityIncident dismiss(UUID id, String note) {
        SecurityIncident incident = requireCurrentTenantIncident(id);
        incident.dismiss(note, OffsetDateTime.now(clock));
        SecurityIncident saved = repository.save(incident);
        auditService.dismissed(saved);
        metrics.dismissed(saved);
        return saved;
    }

    private SecurityIncident requireCurrentTenantIncident(UUID id) {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(SecurityIncidentNotFoundException::new);
    }

    private void ensureAnalysisTenant(SecurityAnalysis analysis, String tenantId) {
        if (analysis == null) {
            throw new IllegalArgumentException("analysis must not be null");
        }
        if (!tenantId.equals(analysis.tenantId())) {
            throw new IllegalArgumentException("Security analysis tenant does not match the authenticated tenant");
        }
    }
}
