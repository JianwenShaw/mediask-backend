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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionMessage;
import me.jianwen.mediask.domain.ai.model.AiSessionSummary;
import me.jianwen.mediask.domain.ai.model.AiSessionSummaryList;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.model.AiSessionTurn;
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
    private static final String PATIENT_USER_ID_HEADER = "X-Patient-User-Id";

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
                    .header(PATIENT_USER_ID_HEADER, String.valueOf(context.actorUserId()))
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

    @Override
    public AiSessionSummaryList listSessions(AiTriageGatewayContext context) {
        requireGatewayConfig();
        try {
            PythonSessionSummaryListResponse response = restClient.get()
                    .uri(uri("/api/v1/sessions"))
                    .headers(headers -> applyHeaders(headers, context))
                    .retrieve()
                    .body(PythonSessionSummaryListResponse.class);
            return toDomain(response == null ? new PythonSessionSummaryListResponse(List.of()) : response);
        } catch (RestClientResponseException exception) {
            throw mapPythonSessionResponseException(exception, false);
        } catch (ResourceAccessException exception) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai session API unavailable", exception);
        } catch (RestClientException exception) {
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai session API response invalid", exception);
        }
    }

    @Override
    public AiSessionDetail getSessionDetail(AiTriageGatewayContext context, String sessionId) {
        requireGatewayConfig();
        try {
            PythonSessionDetailResponse response = restClient.get()
                    .uri(uri("/api/v1/sessions/" + sessionId))
                    .headers(headers -> applyHeaders(headers, context))
                    .retrieve()
                    .body(PythonSessionDetailResponse.class);
            if (response == null) {
                throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai session detail response body is empty");
            }
            return toDomain(response);
        } catch (RestClientResponseException exception) {
            throw mapPythonSessionResponseException(exception, false);
        } catch (ResourceAccessException exception) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai session API unavailable", exception);
        } catch (RestClientException exception) {
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai session API response invalid", exception);
        }
    }

    @Override
    public AiSessionTriageResult getSessionTriageResult(AiTriageGatewayContext context, String sessionId) {
        requireGatewayConfig();
        try {
            PythonSessionTriageResultResponse response = restClient.get()
                    .uri(uri("/api/v1/sessions/" + sessionId + "/triage-result"))
                    .headers(headers -> applyHeaders(headers, context))
                    .retrieve()
                    .body(PythonSessionTriageResultResponse.class);
            if (response == null) {
                throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai session triage result response body is empty");
            }
            return toDomain(response);
        } catch (RestClientResponseException exception) {
            throw mapPythonSessionResponseException(exception, true);
        } catch (ResourceAccessException exception) {
            throw new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai session API unavailable", exception);
        } catch (RestClientException exception) {
            throw new SysException(ErrorCode.AI_RESPONSE_INVALID, "Python ai session API response invalid", exception);
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

    private RuntimeException mapPythonSessionResponseException(
            RestClientResponseException exception, boolean triageResultEndpoint) {
        PythonErrorEnvelope envelope = readErrorEnvelope(exception.getResponseBodyAsString());
        String message = envelope.error() == null ? exception.getStatusText() : envelope.error().message();
        if (exception.getStatusCode().value() == 404) {
            return new BizException(ErrorCode.RESOURCE_NOT_FOUND, message);
        }
        if (triageResultEndpoint && exception.getStatusCode().value() == 409) {
            return new BizException(AiErrorCode.TRIAGE_RESULT_NOT_READY, message);
        }
        if (exception.getStatusCode().is4xxClientError()) {
            return new BizException(ErrorCode.INVALID_PARAMETER, message);
        }
        log.warn("Python ai session request failed, status={}, body={}", exception.getStatusCode(), exception.getResponseBodyAsString());
        return new SysException(ErrorCode.AI_SERVICE_UNAVAILABLE, "Python ai session API unavailable", exception);
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

    private AiSessionSummaryList toDomain(PythonSessionSummaryListResponse response) {
        return new AiSessionSummaryList(response.items() == null
                ? List.of()
                : response.items().stream()
                        .map(item -> new AiSessionSummary(
                                item.sessionId(),
                                item.scene(),
                                item.status(),
                                item.departmentId(),
                                item.chiefComplaintSummary(),
                                item.summary(),
                                item.startedAt(),
                                item.endedAt()))
                        .toList());
    }

    private AiSessionDetail toDomain(PythonSessionDetailResponse response) {
        return new AiSessionDetail(
                response.sessionId(),
                response.scene(),
                response.status(),
                response.departmentId(),
                response.chiefComplaintSummary(),
                response.summary(),
                response.startedAt(),
                response.endedAt(),
                response.turns() == null
                        ? List.of()
                        : response.turns().stream()
                                .map(turn -> new AiSessionTurn(
                                        turn.turnId(),
                                        turn.turnNo(),
                                        turn.turnStatus(),
                                        turn.startedAt(),
                                        turn.completedAt(),
                                        turn.errorCode(),
                                        turn.errorMessage(),
                                        turn.messages() == null
                                                ? List.of()
                                                : turn.messages().stream()
                                                        .map(message -> new AiSessionMessage(
                                                                message.role(),
                                                                message.content(),
                                                                message.createdAt()))
                                                        .toList()))
                                .toList());
    }

    private AiSessionTriageResult toDomain(PythonSessionTriageResultResponse response) {
        return new AiSessionTriageResult(
                response.sessionId(),
                response.resultStatus(),
                response.triageStage(),
                response.riskLevel(),
                response.guardrailAction(),
                response.nextAction(),
                response.finalizedTurnId(),
                response.finalizedAt(),
                response.hasActiveCycle(),
                response.activeCycleTurnNo(),
                response.chiefComplaintSummary(),
                response.recommendedDepartments() == null
                        ? List.of()
                        : response.recommendedDepartments().stream()
                                .map(item -> new AiTriageRecommendedDepartment(
                                        item.departmentId(),
                                        item.departmentName(),
                                        item.priority(),
                                        item.reason()))
                                .toList(),
                response.careAdvice(),
                response.citations() == null
                        ? List.of()
                        : response.citations().stream()
                                .map(item -> new AiTriageCitation(
                                        item.citationOrder(),
                                        item.chunkId(),
                                        item.snippet()))
                                .toList(),
                response.blockedReason(),
                response.catalogVersion());
    }

    private PythonQueryBody toBody(AiTriageQuery query) {
        return new PythonQueryBody("AI_TRIAGE", query.sessionId(), query.hospitalScope(), query.userMessage());
    }

    private void applyHeaders(org.springframework.http.HttpHeaders headers, AiTriageGatewayContext context) {
        headers.set(RequestConstants.REQUEST_ID_HEADER, context.requestId());
        headers.set(API_KEY_HEADER, apiKey);
        headers.set(PATIENT_USER_ID_HEADER, String.valueOf(context.actorUserId()));
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

    private record PythonSessionSummaryListResponse(List<PythonSessionSummary> items) {}

    private record PythonSessionSummary(
            @JsonProperty("session_id") String sessionId,
            String scene,
            String status,
            @JsonProperty("department_id") Long departmentId,
            @JsonProperty("chief_complaint_summary") String chiefComplaintSummary,
            String summary,
            @JsonProperty("started_at") OffsetDateTime startedAt,
            @JsonProperty("ended_at") OffsetDateTime endedAt) {}

    private record PythonSessionDetailResponse(
            @JsonProperty("session_id") String sessionId,
            String scene,
            String status,
            @JsonProperty("department_id") Long departmentId,
            @JsonProperty("chief_complaint_summary") String chiefComplaintSummary,
            String summary,
            @JsonProperty("started_at") OffsetDateTime startedAt,
            @JsonProperty("ended_at") OffsetDateTime endedAt,
            List<PythonSessionTurn> turns) {}

    private record PythonSessionTurn(
            @JsonProperty("turn_id") String turnId,
            @JsonProperty("turn_no") Integer turnNo,
            @JsonProperty("turn_status") String turnStatus,
            @JsonProperty("started_at") OffsetDateTime startedAt,
            @JsonProperty("completed_at") OffsetDateTime completedAt,
            @JsonProperty("error_code") String errorCode,
            @JsonProperty("error_message") String errorMessage,
            List<PythonSessionMessage> messages) {}

    private record PythonSessionMessage(
            String role,
            String content,
            @JsonProperty("created_at") OffsetDateTime createdAt) {}

    private record PythonSessionTriageResultResponse(
            @JsonProperty("session_id") String sessionId,
            @JsonProperty("result_status") String resultStatus,
            @JsonProperty("triage_stage") String triageStage,
            @JsonProperty("risk_level") String riskLevel,
            @JsonProperty("guardrail_action") String guardrailAction,
            @JsonProperty("next_action") String nextAction,
            @JsonProperty("finalized_turn_id") String finalizedTurnId,
            @JsonProperty("finalized_at") OffsetDateTime finalizedAt,
            @JsonProperty("has_active_cycle") Boolean hasActiveCycle,
            @JsonProperty("active_cycle_turn_no") Integer activeCycleTurnNo,
            @JsonProperty("chief_complaint_summary") String chiefComplaintSummary,
            @JsonProperty("recommended_departments") List<PythonRecommendedDepartment> recommendedDepartments,
            @JsonProperty("care_advice") String careAdvice,
            List<PythonCitation> citations,
            @JsonProperty("blocked_reason") String blockedReason,
            @JsonProperty("catalog_version") String catalogVersion) {}

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
