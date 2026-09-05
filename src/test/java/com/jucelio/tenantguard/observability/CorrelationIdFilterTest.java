package com.jucelio.tenantguard.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldReuseIncomingCorrelationIdAndReturnItInResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals("corr-123", MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("corr-123", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String generated = MDC.get(CorrelationIdFilter.MDC_KEY);
            assertNotNull(generated);
            assertFalse(generated.isBlank());
        });

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(responseHeader);
        assertFalse(responseHeader.isBlank());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
