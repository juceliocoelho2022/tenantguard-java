package com.jucelio.tenantguard.securityincident;

import com.jucelio.tenantguard.audit.AuditService;
import org.springframework.stereotype.Service;

@Service
public class SecurityIncidentAuditService {

    static final String RESOURCE_TYPE = "SECURITY_INCIDENT";
    static final String OUTCOME_SUCCESS = "SUCCESS";
    static final String ACTION_OPENED = "SECURITY_INCIDENT_OPENED";
    static final String ACTION_INVESTIGATION_STARTED = "SECURITY_INCIDENT_INVESTIGATION_STARTED";
    static final String ACTION_RESOLVED = "SECURITY_INCIDENT_RESOLVED";
    static final String ACTION_DISMISSED = "SECURITY_INCIDENT_DISMISSED";

    private final AuditService auditService;

    public SecurityIncidentAuditService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void opened(SecurityIncident incident) {
        record(ACTION_OPENED, incident);
    }

    public void investigationStarted(SecurityIncident incident) {
        record(ACTION_INVESTIGATION_STARTED, incident);
    }

    public void resolved(SecurityIncident incident) {
        record(ACTION_RESOLVED, incident);
    }

    public void dismissed(SecurityIncident incident) {
        record(ACTION_DISMISSED, incident);
    }

    private void record(String action, SecurityIncident incident) {
        auditService.record(
                action,
                RESOURCE_TYPE,
                incident.getId().toString(),
                OUTCOME_SUCCESS
        );
    }
}
