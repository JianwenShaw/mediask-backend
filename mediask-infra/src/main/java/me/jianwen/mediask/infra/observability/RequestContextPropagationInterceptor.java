package me.jianwen.mediask.infra.observability;

import java.io.IOException;
import me.jianwen.mediask.common.request.RequestConstants;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class RequestContextPropagationInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String requestId = MDC.get(RequestConstants.MDC_REQUEST_ID);
        if (requestId != null && !requestId.isBlank()) {
            request.getHeaders().set(RequestConstants.REQUEST_ID_HEADER, requestId);
        }
        return execution.execute(request, body);
    }
}
