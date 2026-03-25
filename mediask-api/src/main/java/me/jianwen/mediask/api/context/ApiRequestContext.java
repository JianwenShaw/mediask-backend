package me.jianwen.mediask.api.context;

import jakarta.servlet.http.HttpServletRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.common.request.DefaultRequestIdGenerator;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.common.request.RequestContextSnapshot;
import me.jianwen.mediask.common.request.RequestIdGenerator;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ApiRequestContext {

    private static final RequestIdGenerator REQUEST_ID_GENERATOR = DefaultRequestIdGenerator.INSTANCE;

    private ApiRequestContext() {
    }

    public static String currentRequestIdOrGenerate() {
        return currentRequestContext().requestId();
    }

    public static RequestContextSnapshot currentRequestContext() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new RequestContextSnapshot(REQUEST_ID_GENERATOR.generate(), null, null);
        }

        HttpServletRequest request = attributes.getRequest();
        Object requestId = request.getAttribute(RequestConstants.REQUEST_ID_ATTRIBUTE);
        Object requestUri = request.getAttribute(RequestConstants.REQUEST_URI_ATTRIBUTE);
        String resolvedUserId = resolveCurrentUserId(request);
        if (requestId instanceof String requestIdValue && !requestIdValue.isBlank()) {
            return new RequestContextSnapshot(
                    requestIdValue,
                    requestUri instanceof String requestUriValue ? requestUriValue : request.getRequestURI(),
                    resolvedUserId);
        }

        return new RequestContextSnapshot(
                firstNonBlank(
                        request.getHeader(RequestConstants.REQUEST_ID_HEADER),
                        request.getHeader(RequestConstants.LEGACY_TRACE_ID_HEADER),
                        REQUEST_ID_GENERATOR.generate()),
                request.getRequestURI(),
                resolvedUserId);
    }

    private static String resolveCurrentUserId(HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                        instanceof AuthenticatedUserPrincipal principal) {
            return String.valueOf(principal.userId());
        }

        String mdcUserId = MDC.get(RequestConstants.MDC_USER_ID);
        if (mdcUserId != null && !mdcUserId.isBlank()) {
            return mdcUserId;
        }

        Object requestUserId = request.getAttribute(RequestConstants.USER_ID_ATTRIBUTE);
        if (requestUserId instanceof String userIdValue && !userIdValue.isBlank()) {
            return userIdValue;
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
