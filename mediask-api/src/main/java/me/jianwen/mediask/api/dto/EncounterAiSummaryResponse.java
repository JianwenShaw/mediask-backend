package me.jianwen.mediask.api.dto;

import java.util.List;

public record EncounterAiSummaryResponse(
        Long encounterId,
        Long sessionId,
        String chiefComplaintSummary,
        String structuredSummary,
        String riskLevel,
        List<RecommendedDepartmentResponse> recommendedDepartments,
        List<CitationResponse> latestCitations) {

    public record RecommendedDepartmentResponse(
            Long departmentId, String departmentName, Integer priority, String reason) {}

    public record CitationResponse(Long chunkId, Integer retrievalRank, Double fusionScore, String snippet) {}
}
