package com.jucelio.tenantguard.security.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    List<SecurityEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);

    List<SecurityEvent> findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String tenantId,
            OffsetDateTime createdAt,
            Pageable pageable
    );
}
