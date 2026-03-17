package me.jianwen.mediask.api.context;

import jakarta.servlet.http.HttpServletRequest;
import me.jianwen.mediask.common.constant.CommonConstants;
import me.jianwen.mediask.common.util.RequestIdUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class ApiRequestContext {

    private ApiRequestContext() {
    }

    public static String currentRequestIdOrGenerate() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return RequestIdUtils.generate();
        }

        HttpServletRequest request = attributes.getRequest();
        Object requestId = request.getAttribute(CommonConstants.REQUEST_ID_ATTRIBUTE);
        if (requestId instanceof String requestIdValue && !requestIdValue.isBlank()) {
            return requestIdValue;
        }

        return firstNonBlank(
                request.getHeader(CommonConstants.REQUEST_ID_HEADER),
                request.getHeader(CommonConstants.LEGACY_TRACE_ID_HEADER),
                RequestIdUtils.generate());
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
