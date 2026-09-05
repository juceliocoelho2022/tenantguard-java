package com.jucelio.tenantguard.security.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    List<SecurityEvent> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
