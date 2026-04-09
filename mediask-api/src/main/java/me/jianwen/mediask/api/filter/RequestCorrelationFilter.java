package me.jianwen.mediask.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import me.jianwen.mediask.common.request.DefaultRequestIdGenerator;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.common.request.RequestIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);
    private static final RequestIdGenerator REQUEST_ID_GENERATOR = DefaultRequestIdGenerator.INSTANCE;
    private static final String COMPLETION_LOGGED_ATTRIBUTE =
            RequestCorrelationFilter.class.getName() + ".completionLogged";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = firstNonBlank(
                request.getHeader(RequestConstants.REQUEST_ID_HEADER),
                request.getHeader(RequestConstants.LEGACY_TRACE_ID_HEADER),
                REQUEST_ID_GENERATOR.generate());
        long startedAtNanos = System.nanoTime();
        // Servlet threads are pooled. Preserve any pre-existing MDC so this filter can leave the thread
        // exactly as it found it after binding per-request correlation keys.
        Map<String, String> previousContext = MDC.getCopyOfContextMap();

        try {
            bindRequestContext(request, response, requestId);
            filterChain.doFilter(request, response);
        } finally {
            if (request.isAsyncStarted()) {
                registerAsyncCompletionLogging(request, response, startedAtNanos);
            } else {
                logRequestCompletion(request, response, startedAtNanos);
            }
            restorePreviousContext(previousContext);
        }
    }

    private void bindRequestContext(HttpServletRequest request, HttpServletResponse response, String requestId) {
        request.setAttribute(RequestConstants.REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(RequestConstants.REQUEST_URI_ATTRIBUTE, request.getRequestURI());
        MDC.put(RequestConstants.MDC_REQUEST_ID, requestId);
        MDC.put(RequestConstants.MDC_REQUEST_URI, request.getRequestURI());
        response.setHeader(RequestConstants.REQUEST_ID_HEADER, requestId);
    }

    private void logRequestCompletion(HttpServletRequest request, HttpServletResponse response, long startedAtNanos) {
        if (Boolean.TRUE.equals(request.getAttribute(COMPLETION_LOGGED_ATTRIBUTE))) {
            return;
        }
        request.setAttribute(COMPLETION_LOGGED_ATTRIBUTE, true);

        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            bindCompletionLogContext(request);
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
            log.info(
                    "HTTP request completed, method={}, status={}, durationMs={}",
                    request.getMethod(),
                    response.getStatus(),
                    durationMs);
        } finally {
            restorePreviousContext(previousContext);
        }
    }

    private void registerAsyncCompletionLogging(
            HttpServletRequest request, HttpServletResponse response, long startedAtNanos) {
        try {
            request.getAsyncContext().addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    logRequestCompletion(request, response, startedAtNanos);
                }

                @Override
                public void onTimeout(AsyncEvent event) {}

                @Override
                public void onError(AsyncEvent event) {}

                @Override
                public void onStartAsync(AsyncEvent event) {
                    event.getAsyncContext().addListener(this);
                }
            });
        } catch (IllegalStateException exception) {
            logRequestCompletion(request, response, startedAtNanos);
        }
    }

    private void bindCompletionLogContext(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestConstants.REQUEST_ID_ATTRIBUTE);
        if (requestId instanceof String requestIdValue && !requestIdValue.isBlank()) {
            MDC.put(RequestConstants.MDC_REQUEST_ID, requestIdValue);
        } else {
            MDC.remove(RequestConstants.MDC_REQUEST_ID);
        }

        Object requestUri = request.getAttribute(RequestConstants.REQUEST_URI_ATTRIBUTE);
        if (requestUri instanceof String requestUriValue && !requestUriValue.isBlank()) {
            MDC.put(RequestConstants.MDC_REQUEST_URI, requestUriValue);
        } else {
            MDC.remove(RequestConstants.MDC_REQUEST_URI);
        }

        Object userId = request.getAttribute(RequestConstants.USER_ID_ATTRIBUTE);
        if (userId instanceof String userIdValue && !userIdValue.isBlank()) {
            MDC.put(RequestConstants.MDC_USER_ID, userIdValue);
        } else {
            MDC.remove(RequestConstants.MDC_USER_ID);
        }
    }

    private void restorePreviousContext(Map<String, String> previousContext) {
        if (previousContext == null) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(previousContext);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
