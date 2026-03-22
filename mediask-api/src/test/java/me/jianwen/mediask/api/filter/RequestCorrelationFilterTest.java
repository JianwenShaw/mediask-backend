package me.jianwen.mediask.api.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.FilterChain;
import me.jianwen.mediask.common.request.RequestConstants;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void doFilter_WhenPreviousMdcExists_RestorePreviousContextAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/contracts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestConstants.REQUEST_ID_HEADER, "req_filter_001");
        MDC.put("traceId", "trace_outer_001");

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            assertEquals("req_filter_001", MDC.get(RequestConstants.MDC_REQUEST_ID));
            assertEquals("/api/v1/contracts", MDC.get(RequestConstants.MDC_REQUEST_URI));
            assertEquals("trace_outer_001", MDC.get("traceId"));
            assertEquals("req_filter_001", response.getHeader(RequestConstants.REQUEST_ID_HEADER));
            assertEquals(
                    "req_filter_001",
                    request.getAttribute(RequestConstants.REQUEST_ID_ATTRIBUTE));
            assertEquals(
                    "/api/v1/contracts",
                    request.getAttribute(RequestConstants.REQUEST_URI_ATTRIBUTE));
        };

        try {
            filter.doFilter(request, response, filterChain);
        } finally {
            assertEquals("trace_outer_001", MDC.get("traceId"));
            assertNull(MDC.get(RequestConstants.MDC_REQUEST_ID));
            assertNull(MDC.get(RequestConstants.MDC_REQUEST_URI));
            MDC.clear();
        }
    }
}
