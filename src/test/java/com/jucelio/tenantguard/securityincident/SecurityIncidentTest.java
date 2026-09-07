package com.jucelio.tenantguard.securityincident;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityIncidentTest {

    @Test
    void shouldMoveFromOpenToInvestigatingAndResolve() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-09-07T08:00:00-03:00");
        SecurityIncident incident = SecurityIncident.open(
                "TENANT_A",
                SecurityIncidentSeverity.HIGH,
                65,
                "fingerprint-1",
                createdAt
        );

        incident.startInvestigation(createdAt.plusMinutes(5));
        incident.resolve("Access token revoked and user session reviewed", createdAt.plusMinutes(20));

        assertEquals(SecurityIncidentStatus.RESOLVED, incident.getStatus());
        assertEquals("Access token revoked and user session reviewed", incident.getResolutionNote());
        assertEquals(createdAt.plusMinutes(20), incident.getUpdatedAt());
    }

    @Test
    void shouldAllowDismissalFromOpenStatus() {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-07T08:00:00-03:00");
        SecurityIncident incident = SecurityIncident.open(
                "TENANT_A",
                SecurityIncidentSeverity.MEDIUM,
                45,
                "fingerprint-2",
                now
        );

        incident.dismiss("Known administrative test traffic", now.plusMinutes(10));

        assertEquals(SecurityIncidentStatus.DISMISSED, incident.getStatus());
        assertEquals("Known administrative test traffic", incident.getResolutionNote());
    }

    @Test
    void shouldRejectInvalidLifecycleTransitionAfterResolution() {
        OffsetDateTime now = OffsetDateTime.parse("2026-09-07T08:00:00-03:00");
        SecurityIncident incident = SecurityIncident.open(
                "TENANT_A",
                SecurityIncidentSeverity.MEDIUM,
                45,
                "fingerprint-3",
                now
        );

        incident.resolve("Resolved", now.plusMinutes(5));

        assertThrows(
                IllegalStateException.class,
                () -> incident.startInvestigation(now.plusMinutes(10))
        );
    }
}
