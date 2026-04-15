package me.jianwen.mediask.api.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import me.jianwen.mediask.api.assembler.AiAssembler;
import me.jianwen.mediask.api.dto.AiChatRequest;
import me.jianwen.mediask.api.dto.AiChatResponse;
import me.jianwen.mediask.api.dto.AiSessionDetailResponse;
import me.jianwen.mediask.api.dto.AiSessionListResponse;
import me.jianwen.mediask.api.dto.AiSessionRegistrationHandoffResponse;
import me.jianwen.mediask.api.dto.AiSessionTriageResultResponse;
import me.jianwen.mediask.api.dto.AiChatStreamErrorResponse;
import me.jianwen.mediask.api.dto.AiChatStreamRequest;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.ai.command.ChatAiCommand;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionDetailUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionRegistrationHandoffUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionTriageResultUseCase;
import me.jianwen.mediask.application.ai.usecase.ListAiSessionsUseCase;
import me.jianwen.mediask.application.ai.usecase.ChatAiResult;
import me.jianwen.mediask.application.ai.usecase.ChatAiUseCase;
import me.jianwen.mediask.application.ai.command.StreamAiChatCommand;
import me.jianwen.mediask.application.ai.usecase.AiChatStreamResultEvent;
import me.jianwen.mediask.application.ai.usecase.StreamAiChatUseCase;
import me.jianwen.mediask.common.exception.BaseException;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.ErrorCodeType;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import me.jianwen.mediask.common.result.Result;

@RestController
@RequestMapping("/api/v1/ai")
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private static final String TEXT_EVENT_STREAM_UTF8 = "text/event-stream;charset=UTF-8";
    private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final ChatAiUseCase chatAiUseCase;
    private final StreamAiChatUseCase streamAiChatUseCase;
    private final ListAiSessionsUseCase listAiSessionsUseCase;
    private final GetAiSessionDetailUseCase getAiSessionDetailUseCase;
    private final GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase;
    private final GetAiSessionRegistrationHandoffUseCase getAiSessionRegistrationHandoffUseCase;
    private final TaskExecutor aiSseTaskExecutor;

    public AiController(
            ChatAiUseCase chatAiUseCase,
            StreamAiChatUseCase streamAiChatUseCase,
            ListAiSessionsUseCase listAiSessionsUseCase,
            GetAiSessionDetailUseCase getAiSessionDetailUseCase,
            GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase,
            GetAiSessionRegistrationHandoffUseCase getAiSessionRegistrationHandoffUseCase,
            @Qualifier("aiSseTaskExecutor") TaskExecutor aiSseTaskExecutor) {
        this.chatAiUseCase = chatAiUseCase;
        this.streamAiChatUseCase = streamAiChatUseCase;
        this.listAiSessionsUseCase = listAiSessionsUseCase;
        this.getAiSessionDetailUseCase = getAiSessionDetailUseCase;
        this.getAiSessionTriageResultUseCase = getAiSessionTriageResultUseCase;
        this.getAiSessionRegistrationHandoffUseCase = getAiSessionRegistrationHandoffUseCase;
        this.aiSseTaskExecutor = aiSseTaskExecutor;
    }

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(
            @RequestBody AiChatRequest request, @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePatientPrincipal(principal);
        if (Boolean.TRUE.equals(request.useStream())) {
            throw new BizException(ErrorCode.INVALID_PARAMETER, "useStream must be false for /api/v1/ai/chat");
        }
        ChatAiCommand command = AiAssembler.toChatAiCommand(principal.userId(), request);
        ChatAiResult result = chatAiUseCase.handle(command);
        return Result.ok(AiAssembler.toChatResponse(result));
    }

    @GetMapping("/sessions")
    public Result<AiSessionListResponse> getSessions(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePatientPrincipal(principal);
        return Result.ok(AiAssembler.toAiSessionListResponse(
                listAiSessionsUseCase.handle(AiAssembler.toListAiSessionsQuery(principal.userId()))));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<AiSessionDetailResponse> getSession(
            @PathVariable Long sessionId, @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePatientPrincipal(principal);
        return Result.ok(AiAssembler.toAiSessionDetailResponse(
                getAiSessionDetailUseCase.handle(AiAssembler.toGetAiSessionDetailQuery(principal.userId(), sessionId))));
    }

    @GetMapping("/sessions/{sessionId}/triage-result")
    public Result<AiSessionTriageResultResponse> getTriageResult(
            @PathVariable Long sessionId, @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePatientPrincipal(principal);
        return Result.ok(AiAssembler.toSessionTriageResultResponse(getAiSessionTriageResultUseCase.handle(
                AiAssembler.toGetAiSessionTriageResultQuery(principal.userId(), sessionId))));
    }

    @PostMapping("/sessions/{sessionId}/registration-handoff")
    public Result<AiSessionRegistrationHandoffResponse> getRegistrationHandoff(
            @PathVariable Long sessionId, @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePatientPrincipal(principal);
        return Result.ok(AiAssembler.toAiSessionRegistrationHandoffResponse(
                getAiSessionRegistrationHandoffUseCase.handle(
                        AiAssembler.toGetAiSessionRegistrationHandoffQuery(principal.userId(), sessionId))));
    }

    @PostMapping(path = "/chat/stream", produces = TEXT_EVENT_STREAM_UTF8)
    public SseEmitter stream(
            @RequestBody AiChatStreamRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        requirePatientPrincipal(principal);
        StreamAiChatCommand command = AiAssembler.toStreamAiChatCommand(principal.userId(), request);
        SseEmitter emitter = createEmitter();
        try {
            aiSseTaskExecutor.execute(() -> streamInternal(command, emitter));
        } catch (TaskRejectedException exception) {
            log.error("AI stream task rejected", exception);
            safeSendError(emitter, ErrorCode.SYSTEM_ERROR);
        }
        return emitter;
    }

    SseEmitter createEmitter() {
        return new SseEmitter(0L);
    }

    private void streamInternal(StreamAiChatCommand command, SseEmitter emitter) {
        AtomicBoolean terminalEventSent = new AtomicBoolean(false);
        try {
            streamAiChatUseCase.handle(command, event -> handleStreamEvent(emitter, event, terminalEventSent));
        } catch (Exception exception) {
            if (terminalEventSent.compareAndSet(false, true)) {
                ErrorCodeType errorCode = resolveErrorCode(exception);
                log.error("AI stream failed, code={}, message={}", errorCode.getCode(), errorCode.getMessage(), exception);
                safeSendError(emitter, errorCode);
            }
        }
    }

    private void handleStreamEvent(SseEmitter emitter, AiChatStreamResultEvent event, AtomicBoolean terminalEventSent) {
        if (terminalEventSent.get()) {
            return;
        }
        try {
            switch (event) {
                case AiChatStreamResultEvent.Message message ->
                        emitter.send(SseEmitter.event().name("message").data(message.content(), TEXT_PLAIN_UTF8));
                case AiChatStreamResultEvent.Meta meta -> emitter.send(SseEmitter.event()
                        .name("meta")
                        .data(AiAssembler.toStreamMetaResponse(
                                meta.sessionId(), meta.turnId(), meta.triageResult())));
                case AiChatStreamResultEvent.End ignored -> sendTerminalEvent(
                        emitter, terminalEventSent, SseEmitter.event().name("end"));
                case AiChatStreamResultEvent.Error error -> sendTerminalEvent(
                        emitter,
                        terminalEventSent,
                        SseEmitter.event()
                                .name("error")
                                .data(new AiChatStreamErrorResponse(error.code(), error.message())));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to write ai stream event", exception);
        }
    }

    private void sendTerminalEvent(
            SseEmitter emitter, AtomicBoolean terminalEventSent, SseEmitter.SseEventBuilder terminalEvent) {
        terminalEventSent.set(true);
        try {
            emitter.send(terminalEvent);
            emitter.complete();
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void safeSendError(SseEmitter emitter, ErrorCodeType errorCode) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new AiChatStreamErrorResponse(errorCode.getCode(), errorCode.getMessage())));
            emitter.complete();
        } catch (IOException ioException) {
            emitter.completeWithError(ioException);
        }
    }

    private ErrorCodeType resolveErrorCode(Exception exception) {
        if (exception instanceof BaseException baseException) {
            return baseException.getErrorCode();
        }
        return ErrorCode.SYSTEM_ERROR;
    }

    private void requirePatientPrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
    }
}
