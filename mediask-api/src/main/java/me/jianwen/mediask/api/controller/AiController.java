package me.jianwen.mediask.api.controller;

import me.jianwen.mediask.api.assembler.AiAssembler;
import me.jianwen.mediask.api.dto.AiChatRequest;
import me.jianwen.mediask.api.dto.AiChatResponse;
import me.jianwen.mediask.api.dto.AiSessionDetailResponse;
import me.jianwen.mediask.api.dto.AiSessionListResponse;
import me.jianwen.mediask.api.dto.AiSessionRegistrationHandoffResponse;
import me.jianwen.mediask.api.dto.AiSessionTriageResultResponse;
import me.jianwen.mediask.api.security.AuthenticatedUserPrincipal;
import me.jianwen.mediask.application.ai.command.ChatAiCommand;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionDetailUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionRegistrationHandoffUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionTriageResultUseCase;
import me.jianwen.mediask.application.ai.usecase.ListAiSessionsUseCase;
import me.jianwen.mediask.application.ai.usecase.ChatAiResult;
import me.jianwen.mediask.application.ai.usecase.ChatAiUseCase;
import me.jianwen.mediask.common.exception.BaseException;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.common.exception.ErrorCodeType;
import me.jianwen.mediask.domain.user.exception.UserErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import me.jianwen.mediask.common.result.Result;

@RestController
@RequestMapping("/api/v1/ai")
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class AiController {

    private final ChatAiUseCase chatAiUseCase;
    private final ListAiSessionsUseCase listAiSessionsUseCase;
    private final GetAiSessionDetailUseCase getAiSessionDetailUseCase;
    private final GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase;
    private final GetAiSessionRegistrationHandoffUseCase getAiSessionRegistrationHandoffUseCase;

    public AiController(
            ChatAiUseCase chatAiUseCase,
            ListAiSessionsUseCase listAiSessionsUseCase,
            GetAiSessionDetailUseCase getAiSessionDetailUseCase,
            GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase,
            GetAiSessionRegistrationHandoffUseCase getAiSessionRegistrationHandoffUseCase) {
        this.chatAiUseCase = chatAiUseCase;
        this.listAiSessionsUseCase = listAiSessionsUseCase;
        this.getAiSessionDetailUseCase = getAiSessionDetailUseCase;
        this.getAiSessionTriageResultUseCase = getAiSessionTriageResultUseCase;
        this.getAiSessionRegistrationHandoffUseCase = getAiSessionRegistrationHandoffUseCase;
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

    private void requirePatientPrincipal(AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.patientId() == null) {
            throw new BizException(UserErrorCode.ROLE_MISMATCH);
        }
    }
}
