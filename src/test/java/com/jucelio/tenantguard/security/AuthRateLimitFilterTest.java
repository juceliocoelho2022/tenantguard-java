package com.jucelio.tenantguard.security;

import com.jucelio.tenantguard.observability.AuthenticationMetrics;
import com.jucelio.tenantguard.security.audit.SecurityEventService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {

    @Test
    void shouldReturn429AndRetryAfterWhenLoginLimitIsExceeded() throws Exception {
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-04T12:00:00Z"),
                ZoneOffset.UTC
        );
        RateLimitStore store = new InMemoryRateLimitStore(clock);
        AuthRateLimitService service = new AuthRateLimitService(1, 10, 60, store);
        SecurityEventService securityEventService = mock(SecurityEventService.class);
        AuthenticationMetrics authenticationMetrics = mock(AuthenticationMetrics.class);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(service, securityEventService, authenticationMetrics);
        AtomicInteger chainCalls = new AtomicInteger();

        MockHttpServletRequest firstRequest = loginRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse,
                (request, response) -> chainCalls.incrementAndGet());

        MockHttpServletRequest secondRequest = loginRequest();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse,
                (request, response) -> chainCalls.incrementAndGet());

        assertEquals(1, chainCalls.get());
        assertEquals(429, secondResponse.getStatus());
        assertEquals("60", secondResponse.getHeader("Retry-After"));
        assertEquals("1", secondResponse.getHeader("X-RateLimit-Limit"));
        assertEquals("0", secondResponse.getHeader("X-RateLimit-Remaining"));
        assertTrue(secondResponse.getContentAsString().contains("rate_limit_exceeded"));

        verify(authenticationMetrics).recordRateLimitBlocked("login");
        verify(securityEventService).record(
                eq(secondRequest),
                eq("RATE_LIMIT_EXCEEDED"),
                eq(429),
                isNull(),
                anyString()
        );
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
