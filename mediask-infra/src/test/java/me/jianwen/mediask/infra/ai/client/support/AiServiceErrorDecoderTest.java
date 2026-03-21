package me.jianwen.mediask.infra.ai.client.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import me.jianwen.mediask.common.request.RequestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class AiServiceErrorDecoderTest {

    private final AiServiceErrorDecoder decoder = new AiServiceErrorDecoder(new ObjectMapper());

    @Test
    void decode_WhenUpstreamBodyMatchesContract_ReturnUpstreamError() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(RequestConstants.REQUEST_ID_HEADER, "req_header_001");

        AiServiceTransportException exception = decoder.decode(
                HttpStatus.SERVICE_UNAVAILABLE,
                headers,
                "{\"code\":6001,\"msg\":\"AI service unavailable\",\"requestId\":\"req_body_001\",\"timestamp\":1761234567890}"
                        .getBytes(StandardCharsets.UTF_8));

        assertEquals(AiServiceTransportException.FailureType.UPSTREAM_ERROR, exception.getFailureType());
        assertEquals(6001, exception.getUpstreamCode());
        assertEquals("AI service unavailable", exception.getMessage());
        assertEquals("req_body_001", exception.getUpstreamRequestId());
    }

    @Test
    void decode_WhenUpstreamBodyUnreadable_ReturnInvalidResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(RequestConstants.REQUEST_ID_HEADER, "req_header_001");

        AiServiceTransportException exception = decoder.decode(
                HttpStatus.BAD_GATEWAY, headers, "not-json".getBytes(StandardCharsets.UTF_8));

        assertEquals(AiServiceTransportException.FailureType.INVALID_RESPONSE, exception.getFailureType());
        assertEquals("req_header_001", exception.getUpstreamRequestId());
        assertEquals("ai service returned unreadable error response", exception.getMessage());
    }
}
