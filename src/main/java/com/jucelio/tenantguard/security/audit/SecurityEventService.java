package com.jucelio.tenantguard.security.audit;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.tenant.RlsTenantGuard;
import com.jucelio.tenantguard.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class SecurityEventService {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventService.class);

    private final SecurityEventRepository repository;
    private final SecurityEventAuditWriter auditWriter;
    private final RlsTenantGuard rlsTenantGuard;

    public SecurityEventService(
            SecurityEventRepository repository,
            SecurityEventAuditWriter auditWriter,
            RlsTenantGuard rlsTenantGuard) {
        this.repository = repository;
        this.auditWriter = auditWriter;
        this.rlsTenantGuard = rlsTenantGuard;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(HttpServletRequest request, String eventType, int httpStatus, String username, String details) {
        try {
            AuthenticatedUser authenticatedUser = currentUser();
            String effectiveUsername = username != null ? username : authenticatedUser == null ? null : authenticatedUser.username();
            String tenantId = authenticatedUser == null ? null : authenticatedUser.tenantId();
            OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

            if (tenantId == null) {
                rlsTenantGuard.applyAuditWriter();
                auditWriter.insert(
                        eventType,
                        httpStatus,
                        effectiveUsername,
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getRemoteAddr(),
                        MDC.get("requestId"),
                        MDC.get("traceId"),
                        details,
                        createdAt
                );
                return;
            }

            rlsTenantGuard.applyTenant(tenantId);
            repository.save(new SecurityEvent(
                    eventType,
                    httpStatus,
                    tenantId,
                    effectiveUsername,
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    MDC.get("requestId"),
                    MDC.get("traceId"),
                    details,
                    createdAt
            ));
        } catch (Exception ex) {
            log.warn("security_event_audit_failed eventType={} status={} path={}",
                    eventType, httpStatus, request.getRequestURI());
        }
    }

    @Transactional(readOnly = true)
    public List<SecurityEventResponse> findCurrentTenantEvents(int lookbackHours, int limit) {
        String tenantId = TenantContext.getTenant();
        rlsTenantGuard.applyTenant(tenantId);
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusHours(lookbackHours);

        return repository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        tenantId,
                        cutoff,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(SecurityEventResponse::from)
                .toList();
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }
}
