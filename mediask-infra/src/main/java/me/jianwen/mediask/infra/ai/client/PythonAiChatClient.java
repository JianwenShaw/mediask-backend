package me.jianwen.mediask.infra.ai.client;

import me.jianwen.mediask.infra.ai.client.dto.PythonChatRequest;
import me.jianwen.mediask.infra.ai.client.dto.PythonChatResponse;
import me.jianwen.mediask.infra.ai.client.support.AiServiceTransportException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class PythonAiChatClient {

    private static final String CHAT_ENDPOINT = "/api/v1/chat";

    private final RestClient aiServiceRestClient;

    public PythonAiChatClient(RestClient aiServiceRestClient) {
        this.aiServiceRestClient = aiServiceRestClient;
    }

    public PythonChatResponse chat(PythonChatRequest request) {
        try {
            PythonChatResponse response = aiServiceRestClient.post()
                    .uri(CHAT_ENDPOINT)
                    .body(request)
                    .retrieve()
                    .body(PythonChatResponse.class);
            if (response == null) {
                throw AiServiceTransportException.invalidResponse("ai response body is empty", null);
            }
            return response;
        } catch (AiServiceTransportException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw AiServiceTransportException.fromResourceAccessException(exception);
        } catch (RestClientException exception) {
            throw AiServiceTransportException.invalidResponse("ai response invalid", exception);
        }
    }
}
