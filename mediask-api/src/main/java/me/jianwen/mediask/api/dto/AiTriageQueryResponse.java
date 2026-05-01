package me.jianwen.mediask.api.dto;

import java.util.List;

public record AiTriageQueryResponse(
        String requestId,
        String sessionId,
        String turnId,
        String queryRunId,
        TriageResultResponse triageResult) {

    public record TriageResultResponse(
            String triageStage,
            String triageCompletionReason,
            String nextAction,
            String riskLevel,
            String chiefComplaintSummary,
            List<String> followUpQuestions,
            List<RecommendedDepartmentResponse> recommendedDepartments,
            String careAdvice,
            String blockedReason,
            String catalogVersion,
            List<CitationResponse> citations) {}

    public record RecommendedDepartmentResponse(
            String departmentId,
            String departmentName,
            Integer priority,
            String reason) {}

    public record CitationResponse(
            Integer citationOrder,
            String chunkId,
            String snippet) {}
}
