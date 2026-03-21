package me.jianwen.mediask.infra.ai.client.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import me.jianwen.mediask.common.request.RequestConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

public final class AiServiceErrorDecoder {

    private final ObjectMapper objectMapper;

    public AiServiceErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiServiceTransportException decode(HttpStatusCode statusCode, HttpHeaders headers, byte[] responseBody) {
        String upstreamRequestId = normalize(headers.getFirst(RequestConstants.REQUEST_ID_HEADER));
        if (responseBody == null || responseBody.length == 0) {
            return AiServiceTransportException.upstreamFailure(
                    statusCode, null, defaultMessage(statusCode), upstreamRequestId, null);
        }

        try {
            AiServiceErrorResponse errorResponse = objectMapper.readValue(responseBody, AiServiceErrorResponse.class);
            return AiServiceTransportException.upstreamFailure(
                    statusCode,
                    errorResponse.code(),
                    firstNonBlank(errorResponse.msg(), defaultMessage(statusCode)),
                    firstNonBlank(errorResponse.requestId(), upstreamRequestId),
                    null);
        } catch (IOException exception) {
            return AiServiceTransportException.invalidResponse(
                    statusCode,
                    upstreamRequestId,
                    "ai service returned unreadable error response",
                    exception);
        }
    }

    private String defaultMessage(HttpStatusCode statusCode) {
        if (statusCode.is5xxServerError()) {
            return "ai service unavailable";
        }
        if (statusCode.is4xxClientError()) {
            return "ai service request rejected";
        }
        return "ai response invalid";
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = normalize(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return normalize(second);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
