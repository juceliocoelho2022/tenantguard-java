package com.jucelio.tenantguard.audit;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.tenant.RlsTenantGuard;
import com.jucelio.tenantguard.tenant.TenantContext;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final RlsTenantGuard rlsTenantGuard;

    public AuditService(AuditEventRepository repository, RlsTenantGuard rlsTenantGuard) {
        this.repository = repository;
        this.rlsTenantGuard = rlsTenantGuard;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String resourceType, String resourceId, String outcome) {
        String tenantId = TenantContext.getTenant();
        AuthenticatedUser user = currentUser();
        rlsTenantGuard.applyCurrentTenant();

        AuditEvent event = new AuditEvent(
                tenantId,
                user.username(),
                action,
                resourceType,
                resourceId,
                outcome,
                MDC.get("requestId"),
                MDC.get("traceId"),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        repository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> findCurrentTenantEvents() {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyCurrentTenant();

        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("Authenticated user is required for audit logging.");
        }
        return user;
    }
}
