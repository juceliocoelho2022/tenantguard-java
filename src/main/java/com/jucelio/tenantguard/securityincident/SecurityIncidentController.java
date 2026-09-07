package com.jucelio.tenantguard.securityincident;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/security-incidents")
@SecurityRequirement(name = "bearerAuth")
public class SecurityIncidentController {

    private final SecurityIncidentService service;

    public SecurityIncidentController(SecurityIncidentService service) {
        this.service = service;
    }

    @Operation(summary = "List current tenant security incidents")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant security incidents returned"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not have ROLE_ADMIN")
    })
    @GetMapping
    public List<SecurityIncidentResponse> findAll() {
        return service.findAll().stream()
                .map(SecurityIncidentResponse::from)
                .toList();
    }

    @Operation(summary = "Get current tenant security incident by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Security incident returned"),
            @ApiResponse(responseCode = "404", description = "Incident does not exist for the authenticated tenant")
    })
    @GetMapping("/{id}")
    public SecurityIncidentResponse findById(@PathVariable UUID id) {
        return service.findById(id)
                .map(SecurityIncidentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Security incident not found"));
    }

    @Operation(summary = "Move an open incident to investigation")
    @PatchMapping("/{id}/investigate")
    public SecurityIncidentResponse investigate(@PathVariable UUID id) {
        try {
            return SecurityIncidentResponse.from(service.startInvestigation(id));
        } catch (SecurityIncidentNotFoundException exception) {
            throw notFound(exception);
        } catch (IllegalStateException exception) {
            throw conflict(exception);
        }
    }

    @Operation(summary = "Resolve an active security incident")
    @PatchMapping("/{id}/resolve")
    public SecurityIncidentResponse resolve(
            @PathVariable UUID id,
            @Valid @RequestBody SecurityIncidentResolutionRequest request
    ) {
        try {
            return SecurityIncidentResponse.from(service.resolve(id, request.note()));
        } catch (SecurityIncidentNotFoundException exception) {
            throw notFound(exception);
        } catch (IllegalStateException exception) {
            throw conflict(exception);
        }
    }

    @Operation(summary = "Dismiss an active security incident")
    @PatchMapping("/{id}/dismiss")
    public SecurityIncidentResponse dismiss(
            @PathVariable UUID id,
            @Valid @RequestBody SecurityIncidentResolutionRequest request
    ) {
        try {
            return SecurityIncidentResponse.from(service.dismiss(id, request.note()));
        } catch (SecurityIncidentNotFoundException exception) {
            throw notFound(exception);
        } catch (IllegalStateException exception) {
            throw conflict(exception);
        }
    }

    private ResponseStatusException notFound(SecurityIncidentNotFoundException exception) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
    }

    private ResponseStatusException conflict(IllegalStateException exception) {
        return new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
    }
}
