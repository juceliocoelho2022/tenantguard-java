package com.jucelio.tenantguard.auth;

import com.jucelio.tenantguard.security.AuthenticatedUser;
import com.jucelio.tenantguard.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    private static final Map<String, DemoUser> USERS = Map.of(
            "user-a", new DemoUser("password", "TENANT_A"),
            "user-b", new DemoUser("password", "TENANT_B"),
            "user-c", new DemoUser("password", "TENANT_C")
    );

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
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
                "USER"
        );

        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                demoUser.tenantId()
        );
    }

    private record DemoUser(String password, String tenantId) {}
}
