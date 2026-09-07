package com.jucelio.tenantguard.securityincident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SecurityIncidentResolutionRequest(
        @NotBlank
        @Size(max = 1000)
        String note
) {
}
