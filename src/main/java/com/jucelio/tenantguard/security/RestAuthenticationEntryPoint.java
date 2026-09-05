package com.jucelio.tenantguard.security;

import com.jucelio.tenantguard.security.audit.SecurityEventService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityEventService securityEventService;

    public RestAuthenticationEntryPoint(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        securityEventService.record(request, "AUTHENTICATION_REQUIRED", 401, null, "Protected resource requested without valid authentication");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"unauthorized\",\"message\":\"Autenticação é obrigatória para acessar este recurso.\"}"
        );
    }
}
