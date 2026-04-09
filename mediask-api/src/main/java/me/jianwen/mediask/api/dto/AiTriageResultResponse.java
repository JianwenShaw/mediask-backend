package me.jianwen.mediask.api.dto;

import java.util.List;

public record AiTriageResultResponse(
        String riskLevel,
        String guardrailAction,
        String nextAction,
        String chiefComplaintSummary,
        List<RecommendedDepartmentResponse> recommendedDepartments,
        String careAdvice,
        List<CitationResponse> citations) {

    public record RecommendedDepartmentResponse(
            Long departmentId, String departmentName, Integer priority, String reason) {}

    public record CitationResponse(Long chunkId, Integer retrievalRank, Double fusionScore, String snippet) {}
}
