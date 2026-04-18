package me.jianwen.mediask.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionTriageResultResponse(
        Long sessionId,
        String resultStatus,
        String triageStage,
        String riskLevel,
        String guardrailAction,
        String nextAction,
        Long finalizedTurnId,
        OffsetDateTime finalizedAt,
        boolean hasActiveCycle,
        Integer activeCycleTurnNo,
        String chiefComplaintSummary,
        List<RecommendedDepartmentResponse> recommendedDepartments,
        String careAdvice,
        List<CitationResponse> citations) {

    public record RecommendedDepartmentResponse(
            Long departmentId, String departmentName, Integer priority, String reason) {}

    public record CitationResponse(Long chunkId, Integer retrievalRank, Double fusionScore, String snippet) {}
}
