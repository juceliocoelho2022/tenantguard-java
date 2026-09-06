package com.jucelio.tenantguard.securityintelligence;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/security-intelligence")
@SecurityRequirement(name = "bearerAuth")
public class SecurityIntelligenceController {

    private final SecurityIntelligenceService securityIntelligenceService;

    public SecurityIntelligenceController(SecurityIntelligenceService securityIntelligenceService) {
        this.securityIntelligenceService = securityIntelligenceService;
    }

    @Operation(
            summary = "Analyze current tenant security posture",
            description = "Builds a security analysis using only bounded audit events from the authenticated administrator's tenant."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant security analysis returned"),
            @ApiResponse(responseCode = "400", description = "Invalid lookbackHours; allowed range is 1 to 168"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ROLE_ADMIN")
    })
    @GetMapping
    public SecurityAnalysis analyze(
            @Parameter(description = "Analysis lookback window in hours. Allowed range: 1 to 168.")
            @RequestParam(defaultValue = "24") int lookbackHours
    ) {
        try {
            return securityIntelligenceService.analyzeCurrentTenant(lookbackHours);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
