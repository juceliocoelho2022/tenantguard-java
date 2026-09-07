package com.jucelio.tenantguard.securityincident;

public class SecurityIncidentNotFoundException extends RuntimeException {

    public SecurityIncidentNotFoundException() {
        super("Security incident not found");
    }
}
