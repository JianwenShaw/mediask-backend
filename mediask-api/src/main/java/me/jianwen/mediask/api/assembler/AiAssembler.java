package me.jianwen.mediask.api.assembler;

import java.util.Locale;
import me.jianwen.mediask.api.context.ApiRequestContext;
import me.jianwen.mediask.api.dto.AiChatRequest;
import me.jianwen.mediask.api.dto.AiChatResponse;
import me.jianwen.mediask.api.dto.AiSessionDetailResponse;
import me.jianwen.mediask.api.dto.AiSessionTriageResultResponse;
import me.jianwen.mediask.api.dto.AiChatStreamMetaResponse;
import me.jianwen.mediask.api.dto.AiChatStreamRequest;
import me.jianwen.mediask.api.dto.AiTriageResultResponse;
import me.jianwen.mediask.api.dto.ImportKnowledgeDocumentResponse;
import me.jianwen.mediask.api.dto.KnowledgeBaseResponse;
import me.jianwen.mediask.api.dto.KnowledgeDocumentListItemResponse;
import me.jianwen.mediask.application.ai.command.ChatAiCommand;
import me.jianwen.mediask.application.ai.command.StreamAiChatCommand;
import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.application.ai.usecase.ChatAiResult;
import me.jianwen.mediask.application.ai.usecase.ImportKnowledgeDocumentResult;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentSummary;
import me.jianwen.mediask.domain.ai.model.RiskLevel;

public final class AiAssembler {

    private AiAssembler() {
    }

    public static ChatAiCommand toChatAiCommand(Long patientUserId, AiChatRequest request) {
        return new ChatAiCommand(
                patientUserId,
                request.sessionId(),
                request.message(),
                request.departmentId(),
                toSceneType(request.sceneType()),
                ApiRequestContext.currentRequestIdOrGenerate());
    }

    public static StreamAiChatCommand toStreamAiChatCommand(Long patientUserId, AiChatStreamRequest request) {
        return new StreamAiChatCommand(
                patientUserId,
                request.sessionId(),
                request.message(),
                request.departmentId(),
                toSceneType(request.sceneType()),
                ApiRequestContext.currentRequestIdOrGenerate());
    }

    public static GetAiSessionDetailQuery toGetAiSessionDetailQuery(Long patientUserId, Long sessionId) {
        return new GetAiSessionDetailQuery(patientUserId, sessionId);
    }

    public static GetAiSessionTriageResultQuery toGetAiSessionTriageResultQuery(Long patientUserId, Long sessionId) {
        return new GetAiSessionTriageResultQuery(patientUserId, sessionId);
    }

    public static AiChatResponse toChatResponse(ChatAiResult result) {
        return new AiChatResponse(result.sessionId(), result.turnId(), result.answer(), toTriageResultResponse(result.reply()));
    }

    public static AiChatStreamMetaResponse toStreamMetaResponse(
            Long sessionId, Long turnId, AiChatTriageResult triageResult) {
        return new AiChatStreamMetaResponse(sessionId, turnId, toTriageResultResponse(triageResult));
    }

    public static ImportKnowledgeDocumentResponse toImportKnowledgeDocumentResponse(ImportKnowledgeDocumentResult result) {
        return new ImportKnowledgeDocumentResponse(
                result.documentId(), result.documentUuid(), result.chunkCount(), result.documentStatus());
    }

    public static KnowledgeBaseResponse toKnowledgeBaseResponse(KnowledgeBaseSummary summary) {
        return new KnowledgeBaseResponse(
                summary.id(),
                summary.kbCode(),
                summary.name(),
                summary.ownerType().name(),
                summary.ownerDeptId(),
                summary.visibility().name(),
                summary.status().name(),
                summary.docCount());
    }

    public static KnowledgeDocumentListItemResponse toKnowledgeDocumentListItemResponse(KnowledgeDocumentSummary summary) {
        return new KnowledgeDocumentListItemResponse(
                summary.id(),
                summary.documentUuid(),
                summary.title(),
                summary.sourceType().name(),
                summary.documentStatus().name(),
                summary.chunkCount());
    }

    public static AiSessionDetailResponse toAiSessionDetailResponse(AiSessionDetail detail) {
        return new AiSessionDetailResponse(
                detail.sessionId(),
                detail.sceneType().name(),
                detail.status().name(),
                detail.departmentId(),
                detail.chiefComplaintSummary(),
                detail.summary(),
                detail.startedAt(),
                detail.endedAt(),
                detail.turns().stream()
                        .map(turn -> new AiSessionDetailResponse.AiSessionTurnResponse(
                                turn.turnId(),
                                turn.turnNo(),
                                turn.status().name(),
                                turn.startedAt(),
                                turn.completedAt(),
                                turn.errorCode(),
                                turn.errorMessage(),
                                turn.messages().stream()
                                        .map(message -> new AiSessionDetailResponse.AiSessionMessageResponse(
                                                message.role().name(), message.encryptedContent(), message.createdAt()))
                                        .toList()))
                        .toList());
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

    public static AiSessionTriageResultResponse toSessionTriageResultResponse(AiSessionTriageResultView triageResult) {
        return new AiSessionTriageResultResponse(
                triageResult.sessionId(),
                triageResult.riskLevel().name().toLowerCase(Locale.ROOT),
                triageResult.guardrailAction().name().toLowerCase(Locale.ROOT),
                toNextAction(triageResult.riskLevel(), triageResult.guardrailAction()),
                triageResult.chiefComplaintSummary(),
                triageResult.recommendedDepartments().stream()
                        .map(department -> new AiSessionTriageResultResponse.RecommendedDepartmentResponse(
                                department.departmentId(),
                                department.departmentName(),
                                department.priority(),
                                department.reason()))
                        .toList(),
                triageResult.careAdvice(),
                triageResult.citations().stream()
                        .map(citation -> new AiSessionTriageResultResponse.CitationResponse(
                                citation.chunkId(),
                                citation.retrievalRank(),
                                citation.fusionScore(),
                                citation.snippet()))
                        .toList());
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
