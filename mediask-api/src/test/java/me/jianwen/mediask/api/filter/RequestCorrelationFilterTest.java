package me.jianwen.mediask.api.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.FilterChain;
import me.jianwen.mediask.common.request.RequestConstants;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockAsyncContext;
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

    @Test
    void doFilter_WhenAsyncRequest_DoNotLogCompletionBeforeAsyncEnds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/registrations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAsyncSupported(true);
        request.addHeader(RequestConstants.REQUEST_ID_HEADER, "req_async_001");
        ListAppender<ILoggingEvent> appender = attachAppender();

        FilterChain filterChain = (servletRequest, servletResponse) -> request.startAsync(request, response);

        try {
            filter.doFilter(request, response, filterChain);

            assertTrue(request.isAsyncStarted());
            assertTrue(appender.list.isEmpty());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void doFilter_WhenAsyncRequestCompletes_LogCompletionOnce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/registrations");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAsyncSupported(true);
        request.addHeader(RequestConstants.REQUEST_ID_HEADER, "req_async_002");
        request.setAttribute(RequestConstants.USER_ID_ATTRIBUTE, "user_001");
        ListAppender<ILoggingEvent> appender = attachAppender();

        FilterChain filterChain = (servletRequest, servletResponse) -> request.startAsync(request, response);

        try {
            filter.doFilter(request, response, filterChain);

            MockAsyncContext asyncContext = (MockAsyncContext) request.getAsyncContext();
            assertNotNull(asyncContext);
            assertFalse(asyncContext.getListeners().isEmpty());
            response.setStatus(200);

            for (var listener : asyncContext.getListeners()) {
                listener.onComplete(new AsyncEvent(asyncContext, request, response));
            }

            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.getFirst();
            assertEquals("HTTP request completed, method={}, status={}, durationMs={}", event.getMessage());
            assertEquals("req_async_002", event.getMDCPropertyMap().get(RequestConstants.MDC_REQUEST_ID));
            assertEquals("/api/v1/registrations", event.getMDCPropertyMap().get(RequestConstants.MDC_REQUEST_URI));
            assertEquals("user_001", event.getMDCPropertyMap().get(RequestConstants.MDC_USER_ID));
            assertEquals(3, event.getArgumentArray().length);
            assertEquals("POST", event.getArgumentArray()[0]);
            assertEquals(200, event.getArgumentArray()[1]);
            assertTrue(((Long) event.getArgumentArray()[2]) >= 0L);

            for (var listener : asyncContext.getListeners()) {
                listener.onComplete(new AsyncEvent(asyncContext, request, response));
            }
            assertEquals(1, appender.list.size());
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    void doFilter_WhenAsyncCompletesBeforeListenerRegistration_LogCompletionOnceWithoutFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/registrations") {
            @Override
            public boolean isAsyncStarted() {
                return true;
            }

            @Override
            public jakarta.servlet.AsyncContext getAsyncContext() {
                throw new IllegalStateException("AsyncContext no longer available");
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestConstants.REQUEST_ID_HEADER, "req_async_003");
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            filter.doFilter(request, response, (servletRequest, servletResponse) -> {});

            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.getFirst();
            assertEquals("HTTP request completed, method={}, status={}, durationMs={}", event.getMessage());
            assertEquals("req_async_003", event.getMDCPropertyMap().get(RequestConstants.MDC_REQUEST_ID));
            assertEquals("/api/v1/registrations", event.getMDCPropertyMap().get(RequestConstants.MDC_REQUEST_URI));
        } finally {
            detachAppender(appender);
        }
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestCorrelationFilter.class);
        logger.detachAppender(appender);
        appender.stop();
    }
}
