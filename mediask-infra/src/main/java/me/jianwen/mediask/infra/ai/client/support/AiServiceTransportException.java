package me.jianwen.mediask.infra.ai.client.support;

import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;

public final class AiServiceTransportException extends RuntimeException {

    public enum FailureType {
        UPSTREAM_ERROR,
        TIMEOUT,
        UNAVAILABLE,
        INVALID_RESPONSE
    }

    private final FailureType failureType;
    private final HttpStatusCode httpStatus;
    private final Integer upstreamCode;
    private final String upstreamRequestId;

    private AiServiceTransportException(
            FailureType failureType,
            HttpStatusCode httpStatus,
            Integer upstreamCode,
            String message,
            String upstreamRequestId,
            Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
        this.httpStatus = httpStatus;
        this.upstreamCode = upstreamCode;
        this.upstreamRequestId = normalize(upstreamRequestId);
    }

    public static AiServiceTransportException upstreamFailure(
            HttpStatusCode httpStatus,
            Integer upstreamCode,
            String message,
            String upstreamRequestId,
            Throwable cause) {
        return new AiServiceTransportException(
                FailureType.UPSTREAM_ERROR, httpStatus, upstreamCode, message, upstreamRequestId, cause);
    }

    public static AiServiceTransportException invalidResponse(String message, Throwable cause) {
        return new AiServiceTransportException(FailureType.INVALID_RESPONSE, null, null, message, null, cause);
    }

    public static AiServiceTransportException invalidResponse(
            HttpStatusCode httpStatus, String upstreamRequestId, String message, Throwable cause) {
        return new AiServiceTransportException(
                FailureType.INVALID_RESPONSE, httpStatus, null, message, upstreamRequestId, cause);
    }

    public static AiServiceTransportException fromResourceAccessException(ResourceAccessException exception) {
        if (hasCause(exception, java.net.SocketTimeoutException.class)
                || hasCause(exception, HttpTimeoutException.class)
                || hasCause(exception, TimeoutException.class)) {
            return new AiServiceTransportException(
                    FailureType.TIMEOUT, null, null, "ai service timeout", null, exception);
        }
        return new AiServiceTransportException(
                FailureType.UNAVAILABLE, null, null, "ai service unavailable", null, exception);
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public HttpStatusCode getHttpStatus() {
        return httpStatus;
    }

    public Integer getUpstreamCode() {
        return upstreamCode;
    }

    public String getUpstreamRequestId() {
        return upstreamRequestId;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
