package com.jucelio.tenantguard.security;

import com.jucelio.tenantguard.security.audit.SecurityEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityEventService securityEventService;

    public RestAccessDeniedHandler(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        securityEventService.record(request, "ACCESS_DENIED", 403, null, "Authenticated user lacks required authority");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"forbidden\",\"message\":\"Você não possui permissão para acessar este recurso.\"}"
        );
    }
}
