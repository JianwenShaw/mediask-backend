package me.jianwen.mediask.common.request;

public final class RequestConstants {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String LEGACY_TRACE_ID_HEADER = "X-Trace-Id";

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_URI_ATTRIBUTE = "requestUri";
    public static final String USER_ID_ATTRIBUTE = "userId";
    public static final String ACCESS_TOKEN_ID_ATTRIBUTE = "accessTokenId";
    public static final String ACCESS_TOKEN_EXPIRES_AT_ATTRIBUTE = "accessTokenExpiresAt";

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_REQUEST_URI = "requestUri";
    public static final String MDC_USER_ID = "userId";

    private RequestConstants() {
    }
}
