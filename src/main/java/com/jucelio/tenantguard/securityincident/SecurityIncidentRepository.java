package com.jucelio.tenantguard.securityincident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, UUID> {

    Optional<SecurityIncident> findByIdAndTenantId(UUID id, String tenantId);

    Optional<SecurityIncident> findFirstByTenantIdAndFingerprintAndStatusIn(
            String tenantId,
            String fingerprint,
            List<SecurityIncidentStatus> statuses
    );

    List<SecurityIncident> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);
}
