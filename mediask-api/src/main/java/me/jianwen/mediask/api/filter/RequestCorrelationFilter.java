package me.jianwen.mediask.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

        request.setAttribute(RequestConstants.REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(RequestConstants.REQUEST_URI_ATTRIBUTE, request.getRequestURI());
        MDC.put(RequestConstants.MDC_REQUEST_ID, requestId);
        MDC.put(RequestConstants.MDC_REQUEST_URI, request.getRequestURI());
        response.setHeader(RequestConstants.REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
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
