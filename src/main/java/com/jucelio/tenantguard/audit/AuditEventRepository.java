package com.jucelio.tenantguard.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<AuditEvent> findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String tenantId,
            OffsetDateTime createdAt,
            Pageable pageable
    );
}
