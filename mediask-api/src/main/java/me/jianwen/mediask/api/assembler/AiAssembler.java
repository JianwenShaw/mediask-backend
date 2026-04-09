package me.jianwen.mediask.api.assembler;

import java.util.Locale;
import me.jianwen.mediask.api.dto.AiChatStreamMetaResponse;
import me.jianwen.mediask.api.dto.AiChatStreamRequest;
import me.jianwen.mediask.api.dto.AiTriageResultResponse;
import me.jianwen.mediask.application.ai.command.StreamAiChatCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RiskLevel;

public final class AiAssembler {

    private AiAssembler() {
    }

    public static StreamAiChatCommand toStreamAiChatCommand(AiChatStreamRequest request) {
        return new StreamAiChatCommand(
                request.sessionId(), request.message(), request.departmentId(), toSceneType(request.sceneType()));
    }

    public static AiChatStreamMetaResponse toStreamMetaResponse(
            Long sessionId, Long turnId, AiChatTriageResult triageResult) {
        return new AiChatStreamMetaResponse(sessionId, turnId, toTriageResultResponse(triageResult));
    }

    public static AiTriageResultResponse toTriageResultResponse(AiChatReply reply) {
        return toTriageResultResponse(new AiChatTriageResult(
                reply.chiefComplaintSummary(),
                reply.riskLevel(),
                reply.guardrailAction(),
                reply.recommendedDepartments(),
                reply.careAdvice(),
                reply.citations(),
                reply.executionMetadata()));
    }

    public static AiTriageResultResponse toTriageResultResponse(AiChatTriageResult triageResult) {
        return new AiTriageResultResponse(
                triageResult.riskLevel().name().toLowerCase(Locale.ROOT),
                triageResult.guardrailAction().name().toLowerCase(Locale.ROOT),
                toNextAction(triageResult.riskLevel(), triageResult.guardrailAction()),
                triageResult.chiefComplaintSummary(),
                triageResult.recommendedDepartments().stream()
                        .map(department -> new AiTriageResultResponse.RecommendedDepartmentResponse(
                                department.departmentId(),
                                department.departmentName(),
                                department.priority(),
                                department.reason()))
                        .toList(),
                triageResult.careAdvice(),
                triageResult.citations().stream()
                        .map(citation -> new AiTriageResultResponse.CitationResponse(
                                citation.chunkId(),
                                citation.retrievalRank(),
                                citation.fusionScore(),
                                citation.snippet()))
                        .toList());
    }

    private static AiSceneType toSceneType(String sceneType) {
        if (sceneType == null || sceneType.isBlank()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER, "sceneType is required");
        }
        try {
            return AiSceneType.valueOf(sceneType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_PARAMETER, "sceneType is invalid");
        }
    }

    private static String toNextAction(RiskLevel riskLevel, GuardrailAction guardrailAction) {
        if (riskLevel == RiskLevel.HIGH) {
            return "EMERGENCY_OFFLINE";
        }
        if (guardrailAction == GuardrailAction.REFUSE) {
            return "MANUAL_SUPPORT";
        }
        if (riskLevel == RiskLevel.MEDIUM || guardrailAction == GuardrailAction.CAUTION) {
            return "GO_REGISTRATION";
        }
        return "VIEW_TRIAGE_RESULT";
    }
}
