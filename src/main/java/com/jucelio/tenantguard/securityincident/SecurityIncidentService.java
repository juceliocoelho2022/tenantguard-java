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
    private final Clock clock;

    @Autowired
    public SecurityIncidentService(
            SecurityIncidentPolicy policy,
            SecurityIncidentRepository repository,
            RlsTenantGuard rlsTenantGuard
    ) {
        this(policy, repository, rlsTenantGuard, Clock.systemUTC());
    }

    SecurityIncidentService(
            SecurityIncidentPolicy policy,
            SecurityIncidentRepository repository,
            RlsTenantGuard rlsTenantGuard,
            Clock clock
    ) {
        this.policy = policy;
        this.repository = repository;
        this.rlsTenantGuard = rlsTenantGuard;
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
            return activeIncident;
        }

        SecurityIncident incident = SecurityIncident.open(
                tenantId,
                incidentDecision.severity(),
                incidentDecision.riskScore(),
                incidentDecision.fingerprint(),
                OffsetDateTime.now(clock)
        );

        return Optional.of(repository.save(incident));
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
        return repository.save(incident);
    }

    @Transactional
    public SecurityIncident resolve(UUID id, String note) {
        SecurityIncident incident = requireCurrentTenantIncident(id);
        incident.resolve(note, OffsetDateTime.now(clock));
        return repository.save(incident);
    }

    @Transactional
    public SecurityIncident dismiss(UUID id, String note) {
        SecurityIncident incident = requireCurrentTenantIncident(id);
        incident.dismiss(note, OffsetDateTime.now(clock));
        return repository.save(incident);
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
