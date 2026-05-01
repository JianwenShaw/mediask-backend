package me.jianwen.mediask.infra.ai.adapter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.AiTriageResult;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PythonAiTriageGatewayAdapter implements AiTriageGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(PythonAiTriageGatewayAdapter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;
    private final RestClient restClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public PythonAiTriageGatewayAdapter(
            @Value("${mediask.ai.base-url:}") String baseUrl,
            @Value("${mediask.ai.api-key:}") String apiKey,
            @Value("${mediask.ai.timeout-seconds:30}") int timeoutSeconds,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this(
                baseUrl,
                apiKey,
                timeoutSeconds,
                restClientBuilder
                        .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_1_1)
                                .build()))
                        .build(),
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                objectMapper);
    }

    PythonAiTriageGatewayAdapter(
            String baseUrl,
            String apiKey,
            int timeoutSeconds,
            RestClient restClient,
            HttpClient httpClient,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeoutSeconds = timeoutSeconds;
        this.restClient = restClient;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query) {
        requireGatewayConfig();
        try {
            PythonQueryResponse response = restClient.post()
                    .uri(uri("/api/v1/query"))
                    .headers(headers -> applyHeaders(headers, context))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toBody(query))
                    .retrieve()
                    .body(PythonQueryResponse.class);
            return toDomain(requireResponse(response));
        } catch (RestClientResponseException exception) {
            throw mapPythonResponseException(exception);
        } catch (ResourceAccessException exception) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai triage API unavailable", exception);
        } catch (RestClientException exception) {
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai triage API response invalid", exception);
        }
    }

    @Override
    public void streamQuery(AiTriageGatewayContext context, AiTriageQuery query, StreamEventHandler handler) {
        requireGatewayConfig();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(uri("/api/v1/query/stream"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .header(RequestConstants.REQUEST_ID_HEADER, context.requestId())
                    .header(API_KEY_HEADER, apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(toBody(query))))
                    .build();
        } catch (JsonProcessingException exception) {
            throw new SysException(ErrorCode.SYSTEM_ERROR, "failed to serialize triage query body", exception);
        }
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw mapPythonStreamError(response.statusCode(), response.body());
            }
            parseSse(response.body(), handler);
        } catch (IOException exception) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai triage stream unavailable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SysException(ErrorCode.AI_SERVICE_TIMEOUT, "Python ai triage stream interrupted", exception);
        }
    }

    private PythonQueryResponse requireResponse(PythonQueryResponse response) {
        if (response == null || response.triageResult() == null) {
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai triage API response body is empty");
        }
        return response;
    }

    private void parseSse(InputStream body, StreamEventHandler handler) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            String currentEvent = null;
            List<String> dataLines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    dispatchEvent(currentEvent, dataLines, handler);
                    currentEvent = null;
                    dataLines.clear();
                    continue;
                }
                if (line.startsWith(":")) {
                    continue;
                }
                if (line.startsWith("event:")) {
                    currentEvent = line.substring("event:".length()).trim();
                    continue;
                }
                if (line.startsWith("data:")) {
                    dataLines.add(line.substring("data:".length()).trim());
                }
            }
            dispatchEvent(currentEvent, dataLines, handler);
        }
    }

    private void dispatchEvent(String eventName, List<String> dataLines, StreamEventHandler handler) {
        if (eventName == null) {
            return;
        }
        String data = dataLines.isEmpty() ? "{}" : String.join("\n", dataLines);
        if ("final".equals(eventName)) {
            try {
                handler.onEvent(new StreamEvent(eventName, data, toDomain(requireResponse(
                        objectMapper.readValue(data, PythonQueryResponse.class)))));
            } catch (JsonProcessingException exception) {
                throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai triage final event is invalid", exception);
            }
            return;
        }
        handler.onEvent(new StreamEvent(eventName, data, null));
    }

    private RuntimeException mapPythonStreamError(int statusCode, InputStream body) throws IOException {
        String bodyText = new String(body.readAllBytes(), StandardCharsets.UTF_8);
        PythonErrorEnvelope envelope = readErrorEnvelope(bodyText);
        if (statusCode >= 400 && statusCode < 500) {
            return new BizException(ErrorCode.INVALID_PARAMETER, envelope.error() == null ? "invalid triage request" : envelope.error().message());
        }
        log.warn("Python ai triage stream failed, status={}, body={}", statusCode, bodyText);
        return new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai triage stream failed");
    }

    private RuntimeException mapPythonResponseException(RestClientResponseException exception) {
        PythonErrorEnvelope envelope = readErrorEnvelope(exception.getResponseBodyAsString());
        if (exception.getStatusCode().is4xxClientError()) {
            String message = envelope.error() == null ? "invalid triage request" : envelope.error().message();
            return new BizException(ErrorCode.INVALID_PARAMETER, message);
        }
        log.warn("Python ai triage request failed, status={}, body={}", exception.getStatusCode(), exception.getResponseBodyAsString());
        return new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai triage API unavailable", exception);
    }

    private PythonErrorEnvelope readErrorEnvelope(String bodyText) {
        if (bodyText == null || bodyText.isBlank()) {
            return new PythonErrorEnvelope(null, null);
        }
        try {
            return objectMapper.readValue(bodyText, PythonErrorEnvelope.class);
        } catch (JsonProcessingException exception) {
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai triage error body is invalid", exception);
        }
    }

    private AiTriageQueryResponse toDomain(PythonQueryResponse response) {
        return new AiTriageQueryResponse(
                response.requestId(),
                response.sessionId(),
                response.turnId(),
                response.queryRunId(),
                toDomain(response.triageResult()));
    }

    private AiTriageResult toDomain(PythonTriageResult triageResult) {
        return new AiTriageResult(
                triageResult.triageStage(),
                triageResult.triageCompletionReason(),
                triageResult.nextAction(),
                triageResult.riskLevel(),
                triageResult.chiefComplaintSummary(),
                triageResult.followUpQuestions(),
                triageResult.recommendedDepartments() == null
                        ? List.of()
                        : triageResult.recommendedDepartments().stream()
                                .map(item -> new AiTriageRecommendedDepartment(
                                        item.departmentId(),
                                        item.departmentName(),
                                        item.priority(),
                                        item.reason()))
                                .toList(),
                triageResult.careAdvice(),
                triageResult.blockedReason(),
                triageResult.catalogVersion(),
                triageResult.citations() == null
                        ? List.of()
                        : triageResult.citations().stream()
                                .map(item -> new AiTriageCitation(
                                        item.citationOrder(),
                                        item.chunkId(),
                                        item.snippet()))
                                .toList());
    }

    private PythonQueryBody toBody(AiTriageQuery query) {
        return new PythonQueryBody("AI_TRIAGE", query.sessionId(), query.hospitalScope(), query.userMessage());
    }

    private void applyHeaders(org.springframework.http.HttpHeaders headers, AiTriageGatewayContext context) {
        headers.set(RequestConstants.REQUEST_ID_HEADER, context.requestId());
        headers.set(API_KEY_HEADER, apiKey);
    }

    private URI uri(String path) {
        if (baseUrl.isBlank()) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "mediask.ai.base-url is required");
        }
        return URI.create(baseUrl + path);
    }

    private void requireGatewayConfig() {
        if (baseUrl.isBlank()) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "mediask.ai.base-url is required");
        }
        if (apiKey.isBlank()) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "mediask.ai.api-key is required");
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private record PythonQueryBody(
            String scene,
            @JsonProperty("session_id") String sessionId,
            @JsonProperty("hospital_scope") String hospitalScope,
            @JsonProperty("user_message") String userMessage) {}

    private record PythonQueryResponse(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("session_id") String sessionId,
            @JsonProperty("turn_id") String turnId,
            @JsonProperty("query_run_id") String queryRunId,
            @JsonProperty("triage_result") PythonTriageResult triageResult) {}

    private record PythonTriageResult(
            @JsonProperty("triage_stage") String triageStage,
            @JsonProperty("triage_completion_reason") String triageCompletionReason,
            @JsonProperty("next_action") String nextAction,
            @JsonProperty("risk_level") String riskLevel,
            @JsonProperty("chief_complaint_summary") String chiefComplaintSummary,
            @JsonProperty("follow_up_questions") List<String> followUpQuestions,
            @JsonProperty("recommended_departments") List<PythonRecommendedDepartment> recommendedDepartments,
            @JsonProperty("care_advice") String careAdvice,
            @JsonProperty("blocked_reason") String blockedReason,
            @JsonProperty("catalog_version") String catalogVersion,
            @JsonProperty("citations") List<PythonCitation> citations) {}

    private record PythonRecommendedDepartment(
            @JsonProperty("department_id") Long departmentId,
            @JsonProperty("department_name") String departmentName,
            Integer priority,
            String reason) {}

    private record PythonCitation(
            @JsonProperty("citation_order") Integer citationOrder,
            @JsonProperty("chunk_id") String chunkId,
            String snippet) {}

    private record PythonErrorEnvelope(
            @JsonProperty("request_id") String requestId,
            PythonError error) {}

    private record PythonError(
            String code,
            String message) {}
}
