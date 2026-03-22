package me.jianwen.mediask.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import me.jianwen.mediask.common.request.DefaultRequestIdGenerator;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.common.request.RequestIdGenerator;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final RequestIdGenerator REQUEST_ID_GENERATOR = DefaultRequestIdGenerator.INSTANCE;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = firstNonBlank(
                request.getHeader(RequestConstants.REQUEST_ID_HEADER),
                request.getHeader(RequestConstants.LEGACY_TRACE_ID_HEADER),
                REQUEST_ID_GENERATOR.generate());
        // Servlet threads are pooled. Preserve any pre-existing MDC so this filter can leave the thread
        // exactly as it found it after binding per-request correlation keys.
        Map<String, String> previousContext = MDC.getCopyOfContextMap();

        try {
            bindRequestContext(request, response, requestId);
            filterChain.doFilter(request, response);
        } finally {
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
