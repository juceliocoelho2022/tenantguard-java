package com.jucelio.tenantguard.admin;

import com.jucelio.tenantguard.audit.AuditEventResponse;
import com.jucelio.tenantguard.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AuditService auditService;

    public AdminController(AuditService auditService) {
        this.auditService = auditService;
    }

    @Operation(
            summary = "Check administrative access",
            description = "Returns the administrative status when the authenticated user has ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administrative access authorized"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ROLE_ADMIN")
    })
    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of(
                "status", "ok",
                "message", "Acesso administrativo autorizado."
        );
    }

    @Operation(
            summary = "List tenant audit events",
            description = "Returns only audit events belonging to the authenticated administrator's tenant."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant audit events returned"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ROLE_ADMIN")
    })
    @GetMapping("/audit-events")
    public List<AuditEventResponse> auditEvents() {
        return auditService.findCurrentTenantEvents();
    }
}
