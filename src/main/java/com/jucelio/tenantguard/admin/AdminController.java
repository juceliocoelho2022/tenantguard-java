package com.jucelio.tenantguard.admin;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of(
                "status", "ok",
                "message", "Acesso administrativo autorizado."
        );
    }
}
