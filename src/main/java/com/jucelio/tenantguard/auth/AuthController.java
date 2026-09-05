package com.jucelio.tenantguard.auth;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.security.audit.SecurityEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthTokenService authTokenService;
    private final SecurityEventService securityEventService;

    private static final Map<String, DemoUser> USERS = Map.of(
            "user-a", new DemoUser("password", "TENANT_A", "USER"),
            "user-b", new DemoUser("password", "TENANT_B", "USER"),
            "user-c", new DemoUser("password", "TENANT_C", "USER"),
            "admin-a", new DemoUser("password", "TENANT_A", "ADMIN")
    );

    public AuthController(AuthTokenService authTokenService, SecurityEventService securityEventService) {
        this.authTokenService = authTokenService;
        this.securityEventService = securityEventService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        DemoUser demoUser = USERS.get(request.username());

        if (demoUser == null || !demoUser.password().equals(request.password())) {
            securityEventService.record(httpRequest, "LOGIN_FAILED", 401, request.username(), "Invalid credentials");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }

        var user = new AuthenticatedUser(
                request.username(),
                demoUser.tenantId(),
                demoUser.role()
        );

        return authTokenService.issueTokens(user);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        try {
            return authTokenService.rotate(request.refreshToken());
        } catch (Exception ex) {
            securityEventService.record(httpRequest, "REFRESH_FAILED", 401, null, "Refresh token invalid, expired, revoked, or replayed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido, expirado ou revogado.");
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        try {
            authTokenService.revoke(request.refreshToken());
        } catch (Exception ex) {
            securityEventService.record(httpRequest, "LOGOUT_FAILED", 401, null, "Refresh token invalid, expired, or already revoked");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido, expirado ou revogado.");
        }
    }

    private record DemoUser(String password, String tenantId, String role) {}
}
