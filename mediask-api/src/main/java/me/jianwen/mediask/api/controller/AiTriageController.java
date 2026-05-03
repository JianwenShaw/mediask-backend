package me.jianwen.mediask.api.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import me.jianwen.mediask.api.audit.AuditActionCodes;
import me.jianwen.mediask.api.audit.AuditApiSupport;
import me.jianwen.mediask.api.audit.AuditResourceTypes;
import me.jianwen.mediask.api.dto.AiSessionDetailResponse;
import me.jianwen.mediask.api.dto.AiSessionListResponse;
import me.jianwen.mediask.api.dto.AiSessionTriageResultResponse;
import me.jianwen.mediask.api.assembler.AiTriageAssembler;
import me.jianwen.mediask.api.dto.AiTriageQueryRequest;
import me.jianwen.mediask.api.dto.AiTriageQueryResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionDetailUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionTriageResultUseCase;
import me.jianwen.mediask.application.ai.usecase.ListAiSessionsUseCase;
import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
import me.jianwen.mediask.application.ai.usecase.StreamAiTriageQueryUseCase;
import me.jianwen.mediask.application.ai.usecase.SubmitAiTriageQueryUseCase;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.ErrorCodeCategory;
import me.jianwen.mediask.common.result.Result;
import me.jianwen.mediask.domain.audit.model.DataAccessPurposeCode;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/ai")
public class AiTriageController {

    private final SubmitAiTriageQueryUseCase submitAiTriageQueryUseCase;
    private final StreamAiTriageQueryUseCase streamAiTriageQueryUseCase;
    private final ListAiSessionsUseCase listAiSessionsUseCase;
    private final GetAiSessionDetailUseCase getAiSessionDetailUseCase;
    private final GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase;
    private final ObjectMapper objectMapper;
    private final AuditApiSupport auditApiSupport;

    public AiTriageController(
            SubmitAiTriageQueryUseCase submitAiTriageQueryUseCase,
            StreamAiTriageQueryUseCase streamAiTriageQueryUseCase,
            ListAiSessionsUseCase listAiSessionsUseCase,
            GetAiSessionDetailUseCase getAiSessionDetailUseCase,
            GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase,
            ObjectMapper objectMapper,
            AuditApiSupport auditApiSupport) {
        this.submitAiTriageQueryUseCase = submitAiTriageQueryUseCase;
        this.streamAiTriageQueryUseCase = streamAiTriageQueryUseCase;
        this.listAiSessionsUseCase = listAiSessionsUseCase;
        this.getAiSessionDetailUseCase = getAiSessionDetailUseCase;
        this.getAiSessionTriageResultUseCase = getAiSessionTriageResultUseCase;
        this.objectMapper = objectMapper;
        this.auditApiSupport = auditApiSupport;
    }

    @PostMapping("/triage/query")
    public Result<AiTriageQueryResponse> query(
            @RequestBody AiTriageQueryRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ensurePatient(principal);
        SubmitAiTriageQueryCommand command = AiTriageAssembler.toCommand(principal.userId(), request);
        return Result.ok(AiTriageAssembler.toResponse(submitAiTriageQueryUseCase.handle(command)));
    }

    @GetMapping("/sessions")
    public Result<AiSessionListResponse> listSessions(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ensurePatient(principal);
        return Result.ok(AiTriageAssembler.toResponse(
                listAiSessionsUseCase.handle(AiTriageAssembler.toListSessionsQuery(principal.userId()))));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<AiSessionDetailResponse> getSessionDetail(
            @PathVariable String sessionId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ensurePatient(principal);
        try {
            var detail = getAiSessionDetailUseCase.handle(
                    AiTriageAssembler.toGetSessionDetailQuery(principal.userId(), sessionId),
                    auditApiSupport.currentContext(principal),
                    DataAccessPurposeCode.SELF_SERVICE);
            return Result.ok(AiTriageAssembler.toResponse(detail));
        } catch (BizException exception) {
            recordAiReadFailure(principal, sessionId, exception, AuditActionCodes.AI_SESSION_VIEW_FAILED);
            throw exception;
        }
    }

    @GetMapping("/sessions/{sessionId}/triage-result")
    public Result<AiSessionTriageResultResponse> getSessionTriageResult(
            @PathVariable String sessionId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ensurePatient(principal);
        try {
            return Result.ok(AiTriageAssembler.toResponse(getAiSessionTriageResultUseCase.handle(
                    AiTriageAssembler.toGetSessionTriageResultQuery(principal.userId(), sessionId),
                    auditApiSupport.currentContext(principal),
                    DataAccessPurposeCode.SELF_SERVICE)));
        } catch (BizException exception) {
            recordAiReadFailure(principal, sessionId, exception, AuditActionCodes.AI_TRIAGE_RESULT_VIEW_FAILED);
            throw exception;
        }
    }

    @PostMapping(value = "/triage/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamQuery(
            @RequestBody AiTriageQueryRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        ensurePatient(principal);
        SubmitAiTriageQueryCommand command = AiTriageAssembler.toCommand(principal.userId(), request);
        StreamingResponseBody streamingResponseBody = outputStream -> {
            streamAiTriageQueryUseCase.handle(command, new StreamAiTriageQueryUseCase.StreamEventWriter() {
                @Override
                public void writeEvent(me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort.StreamEvent event) {
                    AiTriageController.this.writeEvent(outputStream, event);
                }

                @Override
                public void writeError(String code, String message) {
                    AiTriageController.this.writeError(outputStream, code, message);
                }
            });
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(streamingResponseBody);
    }

    private void ensurePatient(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
    }

    private void recordAiReadFailure(
            AuthenticatedUserPrincipal principal,
            String sessionId,
            BizException exception,
            String failureActionCode) {
        if (exception.getErrorCode().getCategory() == ErrorCodeCategory.FORBIDDEN) {
            auditApiSupport.recordDeniedDataAccess(
                    AuditResourceTypes.AI_SESSION,
                    sessionId,
                    principal,
                    principal.userId(),
                    null,
                    DataAccessPurposeCode.SELF_SERVICE,
                    String.valueOf(exception.getCode()));
            return;
        }
        auditApiSupport.recordAuditFailure(
                failureActionCode,
                AuditResourceTypes.AI_SESSION,
                sessionId,
                principal,
                String.valueOf(exception.getCode()),
                exception.getMessage(),
                principal.userId(),
                null,
                null);
    }

    private void writeEvent(
            java.io.OutputStream outputStream,
            me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort.StreamEvent event) {
        if (event.isFinal()) {
            writeFrame(outputStream, event.event(), AiTriageAssembler.toResponse(event.finalResponse()));
            return;
        }
        Object payload = switch (event.event()) {
            case "start" -> toStartEvent(read(event.data(), PythonStartEvent.class));
            case "progress" -> read(event.data(), PythonProgressEvent.class);
            case "delta" -> toDeltaEvent(read(event.data(), PythonDeltaEvent.class));
            case "error" -> read(event.data(), ErrorEvent.class);
            case "done" -> read(event.data(), Object.class);
            default -> throw new IllegalStateException("unsupported triage stream event: " + event.event());
        };
        writeFrame(outputStream, event.event(), payload);
    }

    private void writeError(java.io.OutputStream outputStream, String code, String message) {
        writeFrame(outputStream, "error", new ErrorEvent(code, message));
    }

    private StartEvent toStartEvent(PythonStartEvent event) {
        return new StartEvent(event.requestId(), event.sessionId(), event.turnId(), event.queryRunId());
    }

    private DeltaEvent toDeltaEvent(PythonDeltaEvent event) {
        return new DeltaEvent(event.textDelta());
    }

    private <T> T read(String data, Class<T> type) {
        try {
            return objectMapper.readValue(data == null || data.isBlank() ? "{}" : data, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to parse triage stream event", exception);
        }
    }

    private void writeFrame(java.io.OutputStream outputStream, String event, Object payload) {
        try {
            String frame = "event: " + event + "\n"
                    + "data: " + objectMapper.writeValueAsString(payload) + "\n\n";
            outputStream.write(frame.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write triage stream event", exception);
        }
    }

    private record PythonStartEvent(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("session_id") String sessionId,
            @JsonProperty("turn_id") String turnId,
            @JsonProperty("query_run_id") String queryRunId) {}

    private record StartEvent(String requestId, String sessionId, String turnId, String queryRunId) {}

    private record PythonProgressEvent(String step) {}

    private record PythonDeltaEvent(@JsonProperty("text_delta") String textDelta) {}

    private record DeltaEvent(String textDelta) {}

    private record ErrorEvent(String code, String message) {}
}
