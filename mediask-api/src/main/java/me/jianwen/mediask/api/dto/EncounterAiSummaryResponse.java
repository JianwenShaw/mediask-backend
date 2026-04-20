package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record EncounterAiSummaryResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
        @JsonSerialize(using = ToStringSerializer.class) Long sessionId,
        String chiefComplaintSummary,
        String structuredSummary,
        String riskLevel,
        List<RecommendedDepartmentResponse> recommendedDepartments,
        List<CitationResponse> latestCitations) {

    public record RecommendedDepartmentResponse(
            @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
            String departmentName,
            Integer priority,
            String reason) {}

    public record CitationResponse(
            @JsonSerialize(using = ToStringSerializer.class) Long chunkId,
            Integer retrievalRank,
            Double fusionScore,
            String snippet) {}
}
