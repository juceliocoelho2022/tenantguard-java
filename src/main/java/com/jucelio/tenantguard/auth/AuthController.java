package com.jucelio.tenantguard.auth;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthTokenService authTokenService;

    private static final Map<String, DemoUser> USERS = Map.of(
            "user-a", new DemoUser("password", "TENANT_A", "USER"),
            "user-b", new DemoUser("password", "TENANT_B", "USER"),
            "user-c", new DemoUser("password", "TENANT_C", "USER"),
            "admin-a", new DemoUser("password", "TENANT_A", "ADMIN")
    );

    public AuthController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        DemoUser demoUser = USERS.get(request.username());

        if (demoUser == null || !demoUser.password().equals(request.password())) {
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
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            return authTokenService.rotate(request.refreshToken());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido, expirado ou revogado.");
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            authTokenService.revoke(request.refreshToken());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido, expirado ou revogado.");
        }
    }

    private record DemoUser(String password, String tenantId, String role) {}
}
