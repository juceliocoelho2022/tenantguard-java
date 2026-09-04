package com.jucelio.tenantguard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    private final AuthRateLimitService rateLimitService;

    public AuthRateLimitFilter(AuthRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        AuthRateLimitService.RateLimitDecision decision = resolveDecision(request);

        if (decision == null) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader("X-RateLimit-Limit", Integer.toString(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(decision.remaining()));

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"error\":\"rate_limit_exceeded\",\"message\":\"Muitas tentativas. Tente novamente mais tarde.\"}"
        );
    }

    private AuthRateLimitService.RateLimitDecision resolveDecision(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return null;
        }

        String path = request.getRequestURI();
        String clientId = request.getRemoteAddr();

        if (LOGIN_PATH.equals(path)) {
            return rateLimitService.checkLogin(clientId);
        }

        if (REFRESH_PATH.equals(path)) {
            return rateLimitService.checkRefresh(clientId);
        }

        return null;
    }
}
