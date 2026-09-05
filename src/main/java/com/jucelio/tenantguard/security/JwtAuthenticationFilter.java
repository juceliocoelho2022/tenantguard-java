package com.jucelio.tenantguard.security;

import com.jucelio.tenantguard.security.audit.SecurityEventService;
import com.jucelio.tenantguard.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final JwtTenantResolver tenantResolver;
    private final SecurityEventService securityEventService;

    public JwtAuthenticationFilter(JwtTenantResolver tenantResolver, SecurityEventService securityEventService) {
        this.tenantResolver = tenantResolver;
        this.securityEventService = securityEventService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startedAt = System.nanoTime();
        String requestId = resolveRequestId(request);

        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authorization != null && authorization.startsWith("Bearer ")) {
                if (!authenticate(request, authorization.substring(7), response)) {
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            MDC.put("status", Integer.toString(response.getStatus()));
            MDC.put("durationMs", Long.toString(durationMs));
            log.info("request_completed");

            TenantContext.clear();
            MDC.clear();
        }
    }

    private boolean authenticate(HttpServletRequest request, String token, HttpServletResponse response) throws IOException {
        try {
            AuthenticatedUser user = tenantResolver.resolve(token);

            var authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            TenantContext.setTenant(user.tenantId());

            MDC.put("tenant_id", user.tenantId());
            MDC.put("user", user.username());
            MDC.put("role", user.role());
            return true;
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            securityEventService.record(request, "INVALID_TOKEN", 401, null, "JWT token is invalid, expired, or not an access token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"invalid_token\",\"message\":\"Token JWT inválido ou expirado.\"}"
            );
            log.warn("jwt_authentication_failed");
            return false;
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && !incoming.isBlank()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
