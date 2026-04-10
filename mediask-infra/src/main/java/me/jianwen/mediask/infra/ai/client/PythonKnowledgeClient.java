package me.jianwen.mediask.infra.ai.client;

import me.jianwen.mediask.infra.ai.client.dto.PythonKnowledgeIndexRequest;
import me.jianwen.mediask.infra.ai.client.dto.PythonKnowledgePrepareRequest;
import me.jianwen.mediask.infra.ai.client.dto.PythonKnowledgePrepareResponse;
import me.jianwen.mediask.infra.ai.client.support.AiServiceTransportException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public final class PythonKnowledgeClient {

    private static final String PREPARE_ENDPOINT = "/api/v1/knowledge/prepare";
    private static final String INDEX_ENDPOINT = "/api/v1/knowledge/index";

    private final RestClient aiServiceRestClient;

    public PythonKnowledgeClient(RestClient aiServiceRestClient) {
        this.aiServiceRestClient = aiServiceRestClient;
    }

    public PythonKnowledgePrepareResponse prepare(PythonKnowledgePrepareRequest request) {
        try {
            PythonKnowledgePrepareResponse response = aiServiceRestClient.post()
                    .uri(PREPARE_ENDPOINT)
                    .body(request)
                    .retrieve()
                    .body(PythonKnowledgePrepareResponse.class);
            if (response == null || response.chunks() == null) {
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

    public void index(PythonKnowledgeIndexRequest request) {
        try {
            aiServiceRestClient.post()
                    .uri(INDEX_ENDPOINT)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (AiServiceTransportException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw AiServiceTransportException.fromResourceAccessException(exception);
        } catch (RestClientException exception) {
            throw AiServiceTransportException.invalidResponse("ai response invalid", exception);
        }
    }
}
