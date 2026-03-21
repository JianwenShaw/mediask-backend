package me.jianwen.mediask.infra.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import me.jianwen.mediask.common.request.RequestConstants;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class RequestContextPropagationInterceptorTest {

    private final RequestContextPropagationInterceptor interceptor = new RequestContextPropagationInterceptor();

    @Test
    void intercept_WhenRequestIdExistsInMdc_PropagateHeader() throws IOException {
        MutableHttpRequest request = new MutableHttpRequest();
        ClientHttpRequestExecution execution = (httpRequest, body) -> new EmptyClientHttpResponse();
        MDC.put(RequestConstants.MDC_REQUEST_ID, "req_test_001");

        try {
            interceptor.intercept(request, new byte[0], execution);
        } finally {
            MDC.clear();
        }

        assertEquals("req_test_001", request.getHeaders().getFirst(RequestConstants.REQUEST_ID_HEADER));
    }

    private static final class MutableHttpRequest implements HttpRequest {

        private final Map<String, Object> attributes = new HashMap<>();
        private final HttpHeaders headers = new HttpHeaders();

        @Override
        public HttpMethod getMethod() {
            return HttpMethod.POST;
        }

        @Override
        public URI getURI() {
            return URI.create("http://localhost/api/v1/chat");
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }

    private static final class EmptyClientHttpResponse implements ClientHttpResponse {

        @Override
        public org.springframework.http.HttpStatusCode getStatusCode() {
            return HttpStatus.OK;
        }

        @Override
        public String getStatusText() {
            return HttpStatus.OK.getReasonPhrase();
        }

        @Override
        public void close() {
        }

        @Override
        public InputStream getBody() {
            return InputStream.nullInputStream();
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }
    }
}
