package me.jianwen.mediask.api.assembler;

import java.util.List;
import me.jianwen.mediask.api.context.ApiRequestContext;
import me.jianwen.mediask.api.dto.AiTriageQueryRequest;
import me.jianwen.mediask.api.dto.AiTriageQueryResponse;
import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
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
}
