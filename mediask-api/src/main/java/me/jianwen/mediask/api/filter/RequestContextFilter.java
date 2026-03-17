package me.jianwen.mediask.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import me.jianwen.mediask.common.constant.CommonConstants;
import me.jianwen.mediask.common.util.RequestIdUtils;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = firstNonBlank(
                request.getHeader(CommonConstants.REQUEST_ID_HEADER),
                request.getHeader(CommonConstants.LEGACY_TRACE_ID_HEADER),
                RequestIdUtils.generate());

        request.setAttribute(CommonConstants.REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(CommonConstants.REQUEST_URI_ATTRIBUTE, request.getRequestURI());
        MDC.put(CommonConstants.MDC_REQUEST_ID, requestId);
        MDC.put(CommonConstants.MDC_REQUEST_URI, request.getRequestURI());
        response.setHeader(CommonConstants.REQUEST_ID_HEADER, requestId);

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
