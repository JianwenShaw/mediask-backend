package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.context.ApiRequestContext;
import me.jianwen.mediask.api.dto.AiSessionDetailResponse;
import me.jianwen.mediask.api.dto.AiSessionListItemResponse;
import me.jianwen.mediask.api.dto.AiSessionListResponse;
import me.jianwen.mediask.api.dto.AiSessionTriageResultResponse;
import me.jianwen.mediask.api.dto.AiTriageQueryRequest;
import me.jianwen.mediask.api.dto.AiTriageQueryResponse;
import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.application.ai.query.ListAiSessionsQuery;
import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionMessage;
import me.jianwen.mediask.domain.ai.model.AiSessionSummary;
import me.jianwen.mediask.domain.ai.model.AiSessionSummaryList;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.model.AiSessionTurn;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.AiTriageResult;

public final class AiTriageAssembler {

    private static final String DEFAULT_HOSPITAL_SCOPE = "default";

    private AiTriageAssembler() {}

    public static SubmitAiTriageQueryCommand toCommand(Long patientUserId, AiTriageQueryRequest request) {
        if (request == null || request.userMessage() == null || request.userMessage().isBlank()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER, "userMessage is required");
        }
        String hospitalScope = request.hospitalScope() == null || request.hospitalScope().isBlank()
                ? DEFAULT_HOSPITAL_SCOPE
                : request.hospitalScope();
        return new SubmitAiTriageQueryCommand(
                ApiRequestContext.currentRequestIdOrGenerate(),
                patientUserId,
                request.sessionId(),
                hospitalScope,
                request.userMessage());
    }

    public static ListAiSessionsQuery toListSessionsQuery(Long patientUserId) {
        return new ListAiSessionsQuery(ApiRequestContext.currentRequestIdOrGenerate(), patientUserId);
    }

    public static GetAiSessionDetailQuery toGetSessionDetailQuery(Long patientUserId, String sessionId) {
        requireSessionId(sessionId);
        return new GetAiSessionDetailQuery(ApiRequestContext.currentRequestIdOrGenerate(), patientUserId, sessionId);
    }

    public static GetAiSessionTriageResultQuery toGetSessionTriageResultQuery(Long patientUserId, String sessionId) {
        requireSessionId(sessionId);
        return new GetAiSessionTriageResultQuery(ApiRequestContext.currentRequestIdOrGenerate(), patientUserId, sessionId);
    }

    public static AiTriageQueryResponse toResponse(me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse response) {
        AiTriageResult triageResult = response.triageResult();
        return new AiTriageQueryResponse(
                response.requestId(),
                response.sessionId(),
                response.turnId(),
                response.queryRunId(),
                new AiTriageQueryResponse.TriageResultResponse(
                        triageResult.triageStage(),
                        triageResult.triageCompletionReason(),
                        triageResult.nextAction(),
                        triageResult.riskLevel(),
                        triageResult.chiefComplaintSummary(),
                        triageResult.followUpQuestions(),
                        toRecommendedDepartments(triageResult.recommendedDepartments()),
                        triageResult.careAdvice(),
                        triageResult.blockedReason(),
                        triageResult.catalogVersion(),
                        toCitations(triageResult.citations())));
    }

    public static AiSessionListResponse toResponse(AiSessionSummaryList response) {
        return new AiSessionListResponse(response.items() == null ? List.of() : response.items().stream()
                .map(AiTriageAssembler::toSessionListItemResponse)
                .toList());
    }

    public static AiSessionDetailResponse toResponse(AiSessionDetail response) {
        return new AiSessionDetailResponse(
                response.sessionId(),
                response.sceneType(),
                response.status(),
                response.departmentId(),
                response.chiefComplaintSummary(),
                response.summary(),
                response.startedAt(),
                response.endedAt(),
                response.turns() == null ? List.of() : response.turns().stream()
                        .map(AiTriageAssembler::toTurnResponse)
                        .toList());
    }

    public static AiSessionTriageResultResponse toResponse(AiSessionTriageResult response) {
        return new AiSessionTriageResultResponse(
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
                toSessionTriageRecommendedDepartments(response.recommendedDepartments()),
                response.careAdvice(),
                toSessionTriageCitations(response.citations()),
                response.blockedReason(),
                response.catalogVersion());
    }

    private static List<AiTriageQueryResponse.RecommendedDepartmentResponse> toRecommendedDepartments(
            List<AiTriageRecommendedDepartment> departments) {
        return departments == null ? List.of() : departments.stream()
                .map(item -> new AiTriageQueryResponse.RecommendedDepartmentResponse(
                        item.departmentId() == null ? null : String.valueOf(item.departmentId()),
                        item.departmentName(),
                        item.priority(),
                        item.reason()))
                .toList();
    }

    private static List<AiTriageQueryResponse.CitationResponse> toCitations(List<AiTriageCitation> citations) {
        return citations == null ? List.of() : citations.stream()
                .map(item -> new AiTriageQueryResponse.CitationResponse(
                        item.citationOrder(),
                        item.chunkId(),
                        item.snippet()))
                .toList();
    }

    private static AiSessionListItemResponse toSessionListItemResponse(AiSessionSummary item) {
        return new AiSessionListItemResponse(
                item.sessionId(),
                item.sceneType(),
                item.status(),
                item.departmentId(),
                item.chiefComplaintSummary(),
                item.summary(),
                item.startedAt(),
                item.endedAt());
    }

    private static AiSessionDetailResponse.TurnResponse toTurnResponse(AiSessionTurn turn) {
        return new AiSessionDetailResponse.TurnResponse(
                turn.turnId(),
                turn.turnNo(),
                turn.turnStatus(),
                turn.startedAt(),
                turn.completedAt(),
                turn.errorCode(),
                turn.errorMessage(),
                turn.messages() == null ? List.of() : turn.messages().stream()
                        .map(AiTriageAssembler::toMessageResponse)
                        .toList());
    }

    private static AiSessionDetailResponse.MessageResponse toMessageResponse(AiSessionMessage message) {
        return new AiSessionDetailResponse.MessageResponse(
                message.role(),
                message.content(),
                message.createdAt());
    }

    private static List<AiSessionTriageResultResponse.RecommendedDepartmentResponse> toSessionTriageRecommendedDepartments(
            List<AiTriageRecommendedDepartment> departments) {
        return departments == null ? List.of() : departments.stream()
                .map(item -> new AiSessionTriageResultResponse.RecommendedDepartmentResponse(
                        item.departmentId() == null ? null : String.valueOf(item.departmentId()),
                        item.departmentName(),
                        item.priority(),
                        item.reason()))
                .toList();
    }

    private static List<AiSessionTriageResultResponse.CitationResponse> toSessionTriageCitations(
            List<AiTriageCitation> citations) {
        return citations == null ? List.of() : citations.stream()
                .map(item -> new AiSessionTriageResultResponse.CitationResponse(
                        item.citationOrder(),
                        item.chunkId(),
                        item.snippet()))
                .toList();
    }

    private static void requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER, "sessionId is required");
        }
    }
}
