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
        assertGeneratedCorrelationId(null);
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderContainsUnsafeCharacters() throws Exception {
        assertGeneratedCorrelationId("corr id\nforged");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsTooLong() throws Exception {
        assertGeneratedCorrelationId("a".repeat(CorrelationIdFilter.MAX_CORRELATION_ID_LENGTH + 1));
    }

    @Test
    void shouldAcceptSafeCorrelationIdCharacters() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "trace_01:api.v2-ABC");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                assertEquals("trace_01:api.v2-ABC", MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("trace_01:api.v2-ABC", response.getHeader(CorrelationIdFilter.HEADER_NAME));
    }

    private void assertGeneratedCorrelationId(String incoming) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        if (incoming != null) {
            request.addHeader(CorrelationIdFilter.HEADER_NAME, incoming);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            String generated = MDC.get(CorrelationIdFilter.MDC_KEY);
            assertNotNull(generated);
            assertDoesNotThrow(() -> java.util.UUID.fromString(generated));
        });

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(responseHeader);
        assertDoesNotThrow(() -> java.util.UUID.fromString(responseHeader));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
