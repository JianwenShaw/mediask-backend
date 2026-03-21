package me.jianwen.mediask.infra.ai.client.support;

import java.io.IOException;
import me.jianwen.mediask.infra.ai.config.AiServiceProperties;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public final class ApiKeyAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private final String apiKey;

    public ApiKeyAuthenticationInterceptor(AiServiceProperties properties) {
        this.apiKey = properties.apiKey();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        request.getHeaders().set(AiServiceHeaders.API_KEY_HEADER, apiKey);
        return execution.execute(request, body);
    }
}
